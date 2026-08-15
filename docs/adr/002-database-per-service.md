# ADR-002: 服務資料所有權與隔離 (Data Ownership & Isolation)

**Date:** 2026-08-13
**Updated:** 2026-08-15 — 標題與重點由「每服務獨立 DB／共享 instance」改為「資料所有權隔離」；實體部署形式（單/多 instance、schema 或 database）明確降為 infra 細節，不影響核心模型。
**Status:** Accepted

## Context

微服務架構中，資料所有權（data ownership）是服務邊界的核心。若所有服務共用同一份資料（單一 schema、無隔離），會造成：

- **隱性耦合**：任一 service 的 DDL 變更都可能影響其他 service。
- **無獨立擴展**：高併發的 inventory 資料會與低流量的 auth 資料搶同一份資源。
- **單點故障與攻擊面擴大**：單一 DB 掛 = 全系統掛；庫存資料可能因 auth 的弱點被直接讀取。

本專案的資料域（domain）天然可分三塊：**用戶/權限**（auth）、**活動/獎品/抽獎記錄**（campaign）、**庫存/預留**（inventory）。

## Decision

**每個 service 擁有並只操作自己的資料域，跨服務一律經 API / event，禁止直接讀寫他人資料。**

- 各服務的資料以 **schema** 為界（`auth` / `campaign` / `inventory`），table 所有權明確、不共用。
- 跨服務資料交換僅透過 **API 呼叫或 event**（ADR-007）；`prize_id`、`draw_record_id`、`user_id` 在非屬主服務中是**邏輯引用（logical reference）**，不做跨 schema 外鍵。
- 因此**不存在跨服務的 DB transaction**；跨服務一致性由 event + 補償（compensation）+ 對帳（reconciliation）達成最終一致（ADR-006）。

隔離在 **DB 層強制**（非僅紀律）：每服務以專屬 role + `search_path` + GRANT 只授權自有 schema，DB 直接擋下跨 schema 存取。

**實體部署是 infra 細節，非本決策核心**：上述隔離可用「單一 instance 內多 schema」「單一 instance 內多 database」「多 instance」任一形式達成，且可依環境切換，**不影響核心模型**（部署細節見 ADR-008）：

- **dev（地端）**：SQLite / H2（每服務一檔案，天然隔離）。
- **prod（GCP）**：Cloud SQL for PostgreSQL（預設單一 instance 分 schema；需更強隔離或獨立擴容時拆 database/instance，config-only）。

> **用語對照**：修訂前後文件中的「Database-Per-Service」「跨 DB」「跨資料庫」等語，本質皆指「資料所有權隔離、不得跨服務直接存取」；「DB」在此可讀作「schema」，語意不變。

## Consequences

**正面：**

- **服務邊界乾淨**：schema 由各服務全權掌控，DDL 變更不外溢；role 權限在 DB 層強制隔離。
- **獨立擴展與故障隔離**：各資料域可分開調校（index、參數、連線池）並獨立擴容。
- **安全**：攻擊面縮小，庫存資料不會因 auth 弱點被直接讀取。

**負面 / 需付出的代價：**

- **跨服務一致性成本**：抽獎流程涉及 campaign + inventory 兩個資料域（見 ADR-006），需 event + 補償達成最終一致，無法靠單一 DB transaction 保證。
- **SQLite 與 PostgreSQL 語意差異**：SQLite 對 `UPDATE ... WHERE stock > 0` 的行鎖/並發行為與 PostgreSQL 不同，DB 相關併發測試在 prod 前必須以 PostgreSQL（docker-compose / Testcontainers）驗證。
- **migration 成本**：每 schema 一套 migration（共 3 套）。

## Alternatives

- **單一共用 schema + table 前綴隔離**：部署最簡單，但違反資料所有權、schema 演進互相牽制、無法獨立擴展與隔離，否決。
- **Polyglot persistence（不同服務用不同 DB 技術）**：理論可行，但 POC 階段增加運維負擔（多種 DB 要管理、備份、監控），故統一 PostgreSQL，未來有明確需求再個別調整。
- **一律使用嵌入式 DB（即使 prod）**：開發最方便，但缺乏 prod 所需的並發控制與高可用，否決。
