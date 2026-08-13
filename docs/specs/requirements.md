# 需求清單 (Requirements List)

**狀態 (Status):** Accepted
**日期 (Date):** 2026-08-13
**版本 (Version):** 1.1

## 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [README.md](../../README.md) | 專案主說明與原始需求 |
| [ADR 索引](../adr/README.md) | 架構決策紀錄（ADR-001 ~ ADR-009） |
| [系統架構總覽](../architecture/overview.md) | C4 架構圖與技術棧矩陣 |
| [抽獎完整流程](../architecture/draw-flow.md) | 抽獎生命週期與失敗路徑 |
| [風控與併發設計](../architecture/risk-control.md) | Redis key schema、Lua script、冪等與一致性 |
| [部署文件](../architecture/deployment.md) | Cloud Run 拓撲與 go-prod 路徑 |

---

## 0. 原始需求（天條，不可修改）

> ⚠️ **天條**：以下為原始需求原文，**不可修改、不可刪減、不可增補**。所有後續需求、設計與實作，**必須先檢視是否違逆本天條，再進行設計**。下方 §2/§3 的 FR/NFR 是對本天條的**解讀與細化**，僅供落實；若任何解讀與本天條牴觸，**以本天條為準**。

> 🎯 **設計原則**：**不擴增需求，完成最小需求即可**。目標是「系統的完整性與機制健全」，而非堆疊額外功能。任何非天條明列的功能（如保底、即時動態機率、進階授權等）一律**不做**，除非使用者明確指示。**要求快速開發完成**——寧可小而有料，不可大而空泛。

```
設計一個電商轉盤抽獎功能，3種獎品各有N種數量且每個獎品的中獎機率不同，與銘謝惠顧合起來機率為100%，可有同時多次抽獎的機會並包含防止重複抽獎與獎品超抽的情況。

以下為基本要求：

## 功能

### 獎品設定

- 多種獎品且可設定獎品數量（庫存）與對應中獎機率
- 「銘謝惠顧」作為無獎品選項，與各獎品機率總和為 100%
- 獎品內容（名稱、數量、機率等）需可透過動態配置修改

### 抽獎模式

- 支援單次抽獎與多次連續抽獎
- 不同抽獎活動可設定各自的抽獎次數上限

### 風控機制

- 同一使用者不可超出其允許的抽獎次數
- 防止獎品超過庫存被抽取

## 架構

- 採用高可用且可水平擴展的分散式架構，支援快速橫向擴充
- 在高併發場景下，須確保事務一致性
- 資料來源（DataSource）、連線池參數等須可依環境變數切換，支援多環境部署

## 設計

- 前後端分離，後端採用 RESTful API 風格，並提供清晰的路由與參數說明
- API 支援身份驗證與權限分級
- 完整的錯誤流程處理與輸入驗證
- API 說明文件
- Table Schema（DDL + DML or ER Model 等可呈現設計的方式）

## 測試

- 撰寫單元測試，模擬各種機率分布、邊界條件與錯誤場景
```

---

## 1. 系統概觀 (System Overview)

lucky-draw-system 是一個**電商轉盤抽獎微服務平台**，以 Java 21 + Spring Boot 3 打造，目標為高可用、可水平擴展的分散式抽獎系統。核心功能是讓營運人員動態配置「多種獎品（各有庫存與中獎機率）＋銘謝惠顧（總和 100%）」，使用者可進行單次或多次抽獎，並在風控機制下**防止重複抽獎**與**獎品超抽**。

系統由 4 個微服務組成：

| 服務 | 職責 |
|------|------|
| **API Gateway** | 統一入口、JWT 驗證、限流、路由、Idempotency-Key 檢查 |
| **Auth Service** | 使用者註冊/登入、JWT (RS256) 簽發、RBAC 權限管理 |
| **Campaign Service** | 活動與獎品管理、權重抽獎邏輯、冪等控制、發布事件 |
| **Inventory Service** | 庫存預扣、DB 條件更新（真相來源）、補償與對帳 |

本專案定位為 **POC 輕量起步、預留 prod 彈性**：地端以 docker-compose（PostgreSQL/Redis/RabbitMQ）開發，prod 部署於 GCP Cloud Run + Cloud SQL + Memorystore。

---

## 2. 功能需求 (Functional Requirements)

> 需求以 MoSCoW 標註優先級：**Must**（POC 必須）、**Should**（prod 前完成）、**Could**（未來演進）、**Won't**（明確不實作）。描述使用 RFC 2119 語意（MUST / SHOULD / MAY）。

### 2.1 API Gateway

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-GW-01 | 系統 MUST 對所有進入的 `/api/**` 請求驗證 JWT 簽章（RS256 public key）與過期時間 | Must | ADR-009 |
| FR-GW-02 | Gateway MUST 將驗證後的 claims（`X-User-Id`、`X-User-Roles`）以 header 轉發給下游 service，並移除原始 `Authorization` header | Must | ADR-009 |
| FR-GW-03 | 系統 MUST 提供使用者層級與 IP 層級的 Rate Limiting（Redis 計數器），超限回傳 `429 Too Many Requests` | Must | ADR-003, ADR-009 |
| FR-GW-04 | Gateway MUST 檢查 `POST /campaigns/{id}/draw` 是否帶有 `Idempotency-Key` header，缺少時回傳 `400 Bad Request` | Must | ADR-005 |
| FR-GW-05 | Gateway MUST 依路由規則將請求轉發至 auth-service / campaign-service / inventory-service | Must | ADR-001, ADR-009 |
| FR-GW-06 | 公開端點（`POST /auth/login`、`POST /auth/register`、`GET /campaigns`）MUST 不需 token 即可存取 | Must | ADR-009 |

### 2.2 Auth Service

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-AUTH-01 | 系統 MUST 支援使用者註冊（username / email / password） | Must | ADR-009 |
| FR-AUTH-02 | 系統 MUST 支援使用者登入，成功後簽發 JWT（RS256，claims 含 `sub`、`roles`、`exp`、`iat`、`iss`） | Must | ADR-009 |
| FR-AUTH-03 | 系統 MUST 以非對稱簽章隔離簽發與驗證：private key 僅 auth-service 持有，public key 透過 endpoint 公開供各 service 驗證 | Must | ADR-009 |
| FR-AUTH-04 | 系統 SHOULD 支援 token refresh（延長登入有效期） | Should | ADR-009 |
| FR-AUTH-05 | 系統 MUST 支援 RBAC 權限分級（`ROLE_USER` / `ROLE_ADMIN`），admin API 需 `ROLE_ADMIN` | Must | ADR-009 |
| FR-AUTH-06 | 使用者密碼 MUST 以不可逆雜湊（如 BCrypt）儲存，不得明文 | Must | ADR-009 |

### 2.3 Campaign Service（核心）

**活動與獎品管理：**

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-CAMP-01 | 系統 MUST 支援抽獎活動 CRUD，並具活動狀態機：`DRAFT` → `ACTIVE` → `ENDED` | Must | ADR-002 |
| FR-CAMP-02 | 系統 MUST 支援每活動配置多個獎品，每個獎品可設定名稱、庫存數量、中獎機率 | Must | ADR-004 |
| FR-CAMP-03 | 系統 MUST 支援「銘謝惠顧」作為無獎品選項（建模為 `type = THANK_YOU` 的獎品） | Must | ADR-004 |
| FR-CAMP-04 | 系統 MUST 在配置/更新時驗證所有獎品（含銘謝惠顧）機率總和等於 100%（浮點容差內），否則回傳 `400/422` 且不落庫 | Must | ADR-004 |
| FR-CAMP-05 | 系統 MUST 支援動態修改獎品內容（名稱、數量、機率），修改後於後續抽獎生效 | Must | ADR-004 |
| FR-CAMP-06 | 系統 MUST 驗證每個獎品機率介於 `[0, 100]`（非負且不超過 100） | Must | ADR-004 |

**抽獎模式：**

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-CAMP-07 | 系統 MUST 支援**單次抽獎**（`count = 1`） | Must | ADR-004 |
| FR-CAMP-08 | 系統 MUST 支援**批次抽獎**（單一請求 `count = N`，伺服端抽 N 次）：單一 Idempotency-Key 對應整批，DB 落 N 筆 `draw_record`，逐筆各自預扣庫存，回傳 N 筆結果 | Must | ADR-004, ADR-005, ADR-006 |
| FR-CAMP-09 | 系統 MUST 支援**並發多個單次請求**（前端發出 N 個請求、各帶獨立 Idempotency-Key），語意與單次抽獎一致 | Must | ADR-005 |
| FR-CAMP-10 | 系統 MUST 以權重隨機演算法選取獎品：單一 `random double in [0,100)` 走累計機率區間 | Must | ADR-004 |

**抽獎次數上限與冪等：**

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-CAMP-11 | 系統 MUST 支援每活動自訂**活動期間總額**抽獎次數上限（每個使用者每活動整個週期最多 N 次，非每日重置） | Must | ADR-003, ADR-005 |
| FR-CAMP-12 | 系統 MUST 在抽獎前檢查個人剩餘抽獎次數，超過上限回傳 `429 Too Many Requests` | Must | ADR-005 |
| FR-CAMP-13 | 系統 MUST 以複合冪等鍵 `userId + campaignId + idempotencyKey` 防止重複抽獎：Redis SETNX 鎖（第一線）＋ DB `UNIQUE` constraint（最終保證） | Must | ADR-005 |
| FR-CAMP-14 | 系統 MUST 支援 replay 語意：相同複合鍵的重複請求回傳與第一次完全相同的結果，**不重抽、不重扣庫存、不重扣次數** | Must | ADR-005 |
| FR-CAMP-15 | 批次抽獎 `count = N` MUST 一次扣除 N 次抽獎次數（僅成功產生結果的請求計次） | Must | ADR-005 |

**事件發布：**

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-CAMP-17 | 抽中獎品時，系統 MUST 發布 `inventory-commit` 事件（含 `drawRecordId`、`prizeId`、`quantity`）至消息佇列 | Must | ADR-006, ADR-007 |

### 2.4 Inventory Service

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-INV-01 | 系統 MUST 以 Redis Lua script 原子執行熱點庫存預扣（檢查 `> 0` 後 `DECR`），杜絕 read-check-act 競態 | Must | ADR-003, ADR-006 |
| FR-INV-02 | 系統 MUST 消費 `inventory-commit` 事件，並以原子條件更新寫回 DB 真相來源：`UPDATE inventory SET stock = stock - 1 WHERE id = ? AND stock > 0` | Must | ADR-006, ADR-007 |
| FR-INV-03 | 系統 MUST 保證**實際發放獎品數絕不超過庫存**（DB 條件更新為最終保證） | Must | ADR-006 |
| FR-INV-04 | 庫存不足時（Redis 預扣回傳 0），抽獎結果 MUST 降級為銘謝惠顧（不重抽、不發布 inventory-commit） | Must | ADR-006 |
| FR-INV-05 | DB 條件更新影響 0 列時，系統 MUST 執行補償（回滾中獎結果、修正 Redis counter、發出 alert） | Must | ADR-006 |
| FR-INV-06 | consumer MUST 冪等（依 `drawRecordId` 去重），避免消息重複投遞造成重複扣減 | Must | ADR-006, ADR-007 |
| FR-INV-07 | 系統 SHOULD 提供定期對帳 job（以 DB 庫存校正 Redis counter、回收超時預留） | Should | ADR-006 |

### 2.5 交錯需求 (Cross-cutting)

| ID | 需求描述 | 優先級 | 對應 ADR |
|----|----------|--------|----------|
| FR-X-01 | 系統 MUST 提供完整的錯誤流程處理與輸入驗證，明確定義 `400` / `401` / `403` / `404` / `409` / `429` / `500` 語意 | Must | ADR-005, ADR-009 |
| FR-X-02 | 批次抽獎的整批副作用（次數扣除、事件發布）MUST 僅執行一次（由整批的單一 Idempotency-Key 保護） | Must | ADR-005 |
| FR-X-03 | 前後端分離，後端以 RESTful API 風格提供清晰路由與參數說明 | Must | ADR-009 |
| FR-X-04 | 所有 API MUST 提供文件（OpenAPI 3.0 / Swagger） | Must | — |

---

## 3. 非功能需求 (Non-Functional Requirements)

| ID | 需求 | 說明 | 優先級 | 對應 ADR |
|----|------|------|--------|----------|
| NFR-01 | 高可用 (HA) | 多 instance 部署、故障隔離；Cloud SQL / Memorystore 啟用 HA 與 failover | Must | ADR-008 |
| NFR-02 | 水平擴展 | 各 service 獨立縮放，支援快速橫向擴充（抽獎高峰放大 campaign/inventory） | Must | ADR-001, ADR-008 |
| NFR-03 | 高併發一致性 | Redis 加速層（低延遲）＋ DB 真相層（條件更新）＋ 補償對帳，確保事務一致性與最終收斂 | Must | ADR-003, ADR-006 |
| NFR-04 | 多環境配置 | **資料來源 (DataSource)、連線池參數等 MUST 可依環境變數切換**，支援 dev（SQLite/PostgreSQL）/ prod（Cloud SQL）多環境部署 | Must | ADR-002, ADR-008 |
| NFR-05 | 安全性 | JWT RS256、Secret Manager 管理機密、最小權限、defense in depth（Gateway + service 雙層驗證） | Must | ADR-009 |
| NFR-06 | 可觀察性 | 各 service 標準 logging、broker backlog 監控、對帳告警 | Should | ADR-006, ADR-008 |
| NFR-07 | 測試覆蓋 | 單元測試模擬機率分布、邊界條件與錯誤場景；JUnit 5 + Mockito + Testcontainers | Must | — |
| NFR-08 | API 文件與風格 | 前後端分離、RESTful API、清晰路由與參數說明、OpenAPI 3.0 文件 | Must | ADR-009 |

---

## 4. 範圍邊界 (Out of Scope / Won't)

以下項目**明確不納入 POC 範圍**（Won't），保留為 prod 演化方向：

| 項目 | 說明 |
|------|------|
| OAuth2 / OIDC 完整授權流程 | 簡化版的「Auth Service 簽發 JWT」即可，完整 Authorization Server 留待演化（ADR-009） |
| JWT 主動撤銷（blacklist） | 以短 TTL + `token_version` 欄位暫代（ADR-009） |
| 金流 / 發券系統整合 | 中獎發放通知為未來整合項目 |
| 前端應用實作 | 本專案聚焦後端微服務，前端分離但由另一專案實作 |
| 實際 prod 部署 | 僅保留架構彈性與 go-prod 路徑（ADR-008），不實際部署 GCP |

---

## 5. 需求追溯矩陣 (Traceability Matrix)

### 5.1 原始需求 → 本文件

| 使用者原始需求 | 對應 FR / NFR |
|----------------|---------------|
| 多種獎品、可設庫存與機率 | FR-CAMP-02 |
| 銘謝惠顧與總和 100% | FR-CAMP-03, FR-CAMP-04, FR-CAMP-06 |
| 獎品動態配置修改 | FR-CAMP-05 |
| 單次抽獎 | FR-CAMP-07 |
| 多次連續抽獎（批次 + 並發） | FR-CAMP-08, FR-CAMP-09 |
| 各活動自訂抽獎次數上限 | FR-CAMP-11, FR-CAMP-12 |
| 使用者不超抽次數 | FR-CAMP-11, FR-CAMP-12, FR-CAMP-13 |
| 防止獎品超抽 | FR-INV-01 ~ FR-INV-06 |
| 高可用、水平擴展 | NFR-01, NFR-02 |
| 高併發事務一致性 | NFR-03 |
| DataSource / 連線池環境變數切換 | NFR-04 |
| 前後端分離、RESTful | FR-X-03, NFR-08 |
| API 身份驗證與權限分級 | FR-GW-01, FR-AUTH-05 |
| 錯誤流程與輸入驗證 | FR-X-01 |
| API 文件 | FR-X-04, NFR-08 |
| Table Schema（DDL/DML/ER） | 後續 `docs/db/` 交付 |
| 單元測試（機率分布/邊界/錯誤） | NFR-07 |

### 5.2 需求 → 後續交付物

| 需求群組 | 後續交付物 |
|----------|-----------|
| FR-GW-* / FR-AUTH-* / FR-CAMP-* / FR-INV-* | `docs/specs/<service>/` per-service contract |
| API 路由與參數 | `docs/api/` OpenAPI 3.0 規格 |
| Table Schema | `docs/db/`（DDL + DML + ER） |
| 測試策略 | 各 service 單元/整合測試（JUnit 5 + Testcontainers） |

---

## 6. 備註 (Notes)

1. **抽獎次數上限維度**：依最新需求確認，個人抽獎次數上限為**活動期間總額**（非每日）。既有 ADR-003 的 key schema、ADR-005、`docs/architecture/risk-control.md` §2.2、`docs/architecture/draw-flow.md` 中提及「當日/每日」次數的內容已同步修訂為「活動期間總額」（key 改為 `draw_count:{userId}:{campaignId}`，TTL 對齊活動結束時間）。

2. **批次抽獎語意**：`count = N` 的批次抽獎為「單一請求、伺服端 N 次獨立抽選」，整批以單一 Idempotency-Key 保護；與「並發多個單次請求」為兩種並存模式，兩者都需支援。
