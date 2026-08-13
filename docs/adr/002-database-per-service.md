# ADR-002: 每個服務擁有獨立資料庫 (Database-Per-Service)

**Date:** 2026-08-13
**Status:** Accepted

## Context

微服務架構中，資料所有權（data ownership）是服務邊界的核心。若所有服務共用一個 database schema，會造成：

- **隱性耦合**：任何一個 service 的 DDL 變更都可能影響其他 service。
- **無獨立擴展**：高併發的 inventory-service 會與低流量的 auth-service 搶同一 DB 資源。
- **單點故障**：單一 DB 掛掉 = 全系統掛掉。

本專案的資料域（domain）天然可分三塊：**用戶/權限**（auth）、**活動/獎品/抽獎記錄**（campaign）、**庫存/預留**（inventory）。

另外需要考量地端開發體驗：POC 階段希望開發者**不用裝 PostgreSQL 也能跑**（SQLite/H2），但 prod 需要 PostgreSQL 的併發控制能力（`UPDATE ... WHERE stock > 0` 的條件更新語意）。

## Decision

採用 **Database-Per-Service**：每個 service 擁有並只操作自己的資料庫，禁止跨 service 直接存取他人 DB。

資料庫對應與 schema 劃分：

| Service | DB | 核心 Table |
|---------|-----|-----------|
| `auth-service` | Auth DB (PostgreSQL) | `users`、`roles`、`user_roles` |
| `campaign-service` | Campaign DB (PostgreSQL) | `campaigns`、`prizes`、`draw_records` |
| `inventory-service` | Inventory DB (PostgreSQL) | `inventory`（庫存）、`reservations`（預留記錄） |

環境差異透過 **Spring Profiles** 切換：

- **`dev` profile（地端）**：使用 **SQLite**（輕量，免安裝）或 **H2**，搭配 `application-dev.yml`。
- **`prod` profile（GCP）**：使用 **PostgreSQL**，prod 部署於 **GCP Cloud SQL**，搭配 `application-prod.yml`。
- docker-compose 提供本地 PostgreSQL 選項（`docker/docker-compose.yml`），讓需要 PostgreSQL 語意驗證的開發者可以選擇使用。

跨服務資料交換一律透過 **API 呼叫或 Kafka event**（見 ADR-007），不允許直接讀寫他人 DB。

## Consequences

**正面：**

- **服務邊界乾淨**：schema 由各 service 全權掌控，DDL 變更不會外溢。
- **獨立擴展與故障隔離**：inventory DB 可單獨調大規格 / 建立複本，單一 DB 故障不會拖垮整個系統。
- **符合 Domain 隔離**：auth、campaign、inventory 各自的查詢模式差異大（OLTP 高頻 vs 中頻），獨立 DB 讓每個 DB 的 index、參數可獨立調校。
- **安全**：攻擊面縮小，inventory 的庫存資料不會因 auth 的弱點被直接讀取。

**負面 / 需付出的代價：**

- **跨 service 的一致性成本**：抽獎流程涉及 campaign + inventory 兩個 DB（見 ADR-006），需要透過 event + 補償（compensation）達成最終一致性（eventual consistency），無法靠單一 DB transaction 保證。
- **SQLite 與 PostgreSQL 語意差異**：SQLite 對 `UPDATE ... WHERE stock > 0` 的行鎖行為、並發寫入行為與 PostgreSQL 不同，DB 相關的併發測試在 prod 前必須以 PostgreSQL（docker-compose / Testcontainers）驗證。
- **schema migration 成本**：每個 DB 需要各自的 migration（如 Flyway/Liquibase）腳本，共 3 套。

## Alternatives

- **單一共用 DB + table 前綴隔離**：部署最簡單，但違反 service 邊界，schema 演進互相牽制，且無法獨立擴展，與「高可用分散式」目標衝突，否決。
- **Polyglot persistence（不同服務用不同 DB 技術）**：理論上可行，但 POC 階段增加運維負擔（多種 DB 要管理、備份、監控），故統一 PostgreSQL，未來有明確需求再個別調整。
- **一律使用嵌入式 DB（即使 prod）**：開發最方便，但缺乏 prod 所需的並發控制與高可用，否決。
