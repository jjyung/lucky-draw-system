# auth-service — SA 業務需求 (Business Requirements)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SA — Business Requirements（業務行為與語意） |
| **服務範圍** | auth-service（使用者註冊/登入、JWT (RS256) 簽發、RBAC 權限分級、密碼安全儲存） |

### 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [requirements.md](../requirements.md) | 主需求清單，本文件逐一對齊其 `FR-AUTH-*`、`FR-X-01` 與相關 `NFR` |
| [AGENTS.md](../../../AGENTS.md) | 開發流程指引，本文件遵循其 §3 SA 層模板 |
| [ADR-009](../../adr/009-security-jwt-gateway.md) | JWT (RS256) 非對稱簽章、Gateway 驗證、RBAC 分級、private key 存放 |
| [ADR-002](../../adr/002-database-per-service.md) | Database-Per-Service：auth-service 擁有並只操作 Auth DB（`users`/`roles`/`user_roles`） |
| [ADR-008](../../adr/008-deployment-cloud-run.md) | 部署架構：private key 存放於 GCP Secret Manager（僅 auth-service 可讀） |

> **層級界線**：本文件只定義**業務行為與語意**（use case、business rule、business state、acceptance intent、business data dictionary）。API 路由/參數、OpenAPI schema、DB schema/型別/constraint/index、JWT 簽章的 key 管理與演算法實作、refresh token 的儲存格式，屬 **SD 層**，不在本文件範圍；本文件引用之 ADR 中的技術細節僅作為業務語意之佐證，不在此重複設計。

> **最小範圍紀律（天條 §0 設計原則）**：不擴增需求，完成最小需求即可。本文件只涵蓋 `FR-AUTH-01 ~ FR-AUTH-06` 與交錯需求 `FR-X-01`；凡非此清單的能力一律標記為 Out of Scope（§7），不臆造需求。

---

## 2. 系統／服務定位

### 2.1 Problem & Goal

抽獎平台是「前後端分離、分散式微服務」架構，需要一個**無狀態（stateless）**的鑑別機制，讓多個 service 在不共享 session 儲存的前提下持續識別「誰在操作、具備什麼角色」。天條 §0 明列兩條根需求：**「API 支援身份驗證與權限分級」**與**「完整的錯誤流程處理與輸入驗證」**。

auth-service 是此能力的承載者：它回答「**系統如何鑑別使用者身份、授與角色，並以自帶身份與權限的 token 承載之**」。具體由四個能力構成：

1. **註冊**：建立使用者帳號，密碼以不可逆雜湊安全儲存。
2. **登入 + JWT 簽發**：驗證憑證後簽發 RS256 JWT，claims 承載 `sub`/`roles`/`exp`/`iat`/`iss`。
3. **非對稱簽章隔離**：private key 僅 auth-service 持有（唯一簽發者），public key 公開供各 service 驗證——「簽發者」與「驗證者」分離。
4. **RBAC 權限分級**：`ROLE_USER` / `ROLE_ADMIN` 兩級，admin API 需 `ROLE_ADMIN`。

auth-service **不依賴任何 upstream 狀態**：帳號、角色、token 均由本服務自身產生。它簽發的 token 由 Gateway 與各 service 以 public key 獨立驗證（ADR-009 defense in depth），auth-service 不維護 token 有效性的 runtime 狀態。

### 2.2 Actors & User Roles

| Role | 說明 | 主要能力 |
|------|------|----------|
| **GUEST**（未登入訪客） | 尚未持有有效 token 的任何人 | 註冊、登入、取得 public key（公開端點，不需 token） |
| **USER**（`ROLE_USER`） | 一般使用者 | 登入取得 token 後，可抽獎、查個人抽獎記錄（由下游 service 判定） |
| **ADMIN**（`ROLE_ADMIN`） | 營運/管理人員 | 登入取得 token 後，可存取 admin API（活動/獎品/庫存配置，由下游 service 判定） |

> 角色判定之落實：auth-service 在登入簽發時將角色寫入 `roles` claim；Gateway 驗證後以 `X-User-Roles` 轉發（ADR-009），各 service 再以 claims 的 roles 為準做授權（`FR-GW-02`、`FR-AUTH-05`）。auth-service 本體只負責「定義並簽發角色」，不負責下游的逐 API 授權判定。

### 2.3 Business Capabilities（本服務提供的能力）

1. 使用者註冊（建立帳號、密碼不可逆雜湊）
2. 使用者登入並簽發 JWT（RS256）
3. 非對稱簽章隔離（private key 簽發、public key 公開驗證）
4. Token 刷新（Should，延長登入有效期）
5. RBAC 權限分級（`ROLE_USER` / `ROLE_ADMIN`）

---

## 3. Use Cases

> 每個 use case 依 AGENTS.md §3 格式：Use case name / Actor / Precondition / Main flow（編號步驟）/ Business rule / Acceptance intent，並帶 **Traceability** 行標註其實現的 `FR-*`。

---

### UC-1 使用者註冊 (User Registration)

- **Actor:** GUEST
- **Precondition:** 呼叫公開端點（不需 token）；提交的 username / email / password 均已提供。
- **Main flow:**
  1. GUEST 提交 username、email、password。
  2. 系統驗證輸入（必填、email 格式合法、username/password 非空）。
  3. 系統檢查 username 或 email 是否已被使用（唯一性）。
  4. 系統以**不可逆雜湊（BCrypt）**對密碼進行雜湊，雜湊值即為儲存內容。
  5. 系統建立使用者，預設角色為 `ROLE_USER`。
  6. 系統回傳成功結果（**不含**密碼或密碼雜湊）。
- **Business rule:**
  - username 與 email 均**唯一**，不得重複註冊；重複時回傳可理解錯誤（`409`）。
  - 密碼 MUST 以不可逆雜湊（BCrypt）儲存，**任何環節不得以明文**持久化、記錄或回傳。
  - 新註冊使用者**預設 `ROLE_USER`**；使用者不得於註冊時指定或自選角色。
  - 建立失敗（唯一性衝突、雜湊異常）時**不得產生半完成資料**（不留部分帳號）。
- **Acceptance intent:**
  - 合法輸入成功建立帳號，密碼落庫者為 BCrypt 雜湊而非明文。
  - 重複 username 或 email 回傳可理解錯誤，不建立第二個帳號。
  - 非法輸入（缺欄位、email 格式錯、密碼為空）回傳驗證錯誤，不落庫。
- **Traceability:** `FR-AUTH-01`, `FR-AUTH-06`, `FR-X-01`

---

### UC-2 使用者登入並取得 JWT (User Login & JWT Issuance)

- **Actor:** GUEST（已註冊使用者）
- **Precondition:** 呼叫公開端點（不需 token）；提交登入憑證（username 或 email + password）。
- **Main flow:**
  1. GUEST 提交登入憑證。
  2. 系統查找對應帳號，以 BCrypt 比對密碼。
  3. 比對成功 → 系統以 **RS256 private key** 簽發 JWT，claims 含 `sub`（使用者識別）、`roles`（角色清單）、`exp`（過期）、`iat`（簽發）、`iss`（簽發者）。
  4. 系統回傳 access token（Should：另附 refresh token，見 UC-3）。
  5. 比對失敗（密碼錯誤或帳號不存在）→ 系統回傳 `401`。
- **Business rule:**
  - JWT 由 auth-service 以 RS256 **private key** 簽發；其他 service / Gateway / client 僅持有 **public key**，只能驗證、不能簽發（**簽發/驗證隔離**）。
  - claims 語意：`sub` = 使用者識別（userId）、`roles` = 角色清單、`exp` = 過期時間、`iat` = 簽發時間、`iss` = 簽發者識別。
  - public key 透過公開 endpoint 提供（不需 token），供 Gateway 與各 service 驗證（`FR-AUTH-03`）。
  - 登入失敗（憑證錯誤）與**帳號不存在回傳相同的可理解錯誤**（`401`），**不洩漏帳號存在性**。
  - client **不得指定或影響 claims 內容**：`sub`/`roles`/`exp`/`iat`/`iss` 均由伺服端依帳號與簽發時刻決定。
- **Acceptance intent:**
  - 正確憑證取得有效 JWT，且 claims 正確反映該使用者的識別與角色。
  - 密碼錯誤或帳號不存在 → `401`，不簽發 token，錯誤訊息不洩漏存在性。
  - 簽發的 token 能以對應 public key 驗證通過（簽章有效）；private key 無法經任何端點取得。
- **Traceability:** `FR-AUTH-02`, `FR-AUTH-03`, `FR-X-01`

---

### UC-3 Token 刷新 (Token Refresh) — Should

- **Actor:** 已登入使用者（持有 refresh token）
- **Precondition:** 持有尚未過期的 refresh token。
- **Main flow:**
  1. 使用者提交 refresh token。
  2. 系統驗證 refresh token 有效且未過期。
  3. 驗證成功 → 簽發新的 access token（**沿用原 `sub` 與 `roles`**，更新 `iat`/`exp`）。
  4. 回傳新 access token；驗證失敗（過期/無效）→ 回傳 `401`。
- **Business rule:**
  - refresh 僅**延長登入有效期**，不改變身份（`sub`）或權限（`roles`）。
  - refresh token 過期或無效時，使用者**必須重新登入**。
  - 本能力為 **Should**（prod 前完成，非 POC 必須）；POC 未實作不視為違反天條。
- **Acceptance intent:**
  - 有效 refresh token 取得新 access token，身份與角色保持不變。
  - 過期/無效 refresh token → `401`，需重新登入。
- **Traceability:** `FR-AUTH-04`

---

### UC-4 角色與權限分級 (RBAC Role Hierarchy)

- **Actor:** 系統（簽發時）／下游 service（授權判定時）／USER / ADMIN
- **Precondition:** 使用者已完成登入，token 含 `roles` claim。
- **Main flow:**
  1. 登入簽發時，系統依帳號角色填入 `roles` claim（`ROLE_USER` / `ROLE_ADMIN`）。
  2. Gateway 驗證 token 後以 `X-User-Roles` 轉發（ADR-009）。
  3. 各 service 以 claims 的 roles 為準做授權：admin API 需 `ROLE_ADMIN`；一般 API 需 `ROLE_USER`（或公開）。
- **Business rule:**
  - 角色分級為兩級：`ROLE_USER`（一般使用者）與 `ROLE_ADMIN`（管理人員）；admin API 需 `ROLE_ADMIN`。
  - 授權判定以 **JWT claims 的 roles 為準**（ADR-009），不信任 client 傳入的身分或角色。
  - 使用者**不得自行指定、修改或升級角色**：註冊預設 `ROLE_USER`，roles 由 auth-service 於簽發時依帳號決定。
  - `ROLE_ADMIN` 的**授予途徑**：POC 階段以**種子資料（seed data）**預先建立至少一個 ADMIN 帳號，不提供任何提權 API 或公開註冊途徑；ADMIN 帳號的建立/維護屬營運端種子資料，非系統功能。
  - 越權存取 admin API → 下游 service 回傳 `403 Forbidden`。
- **Acceptance intent:**
  - 帶 `ROLE_USER` token 存取 admin API → `403`。
  - 帶 `ROLE_ADMIN` token 存取 admin API → 允許。
  - client 無法透過請求內容篡改 roles（roles 由 token 決定，非請求欄位）。
- **Traceability:** `FR-AUTH-05`, `FR-X-01`

---

## 4. Business State

### 4.1 Token 生命週期 (Token Lifecycle Semantics)

```
（登入成功） ISSUED ──► ACTIVE ──► EXPIRED
                        │
                        └──（Should，refresh）► 重新簽發新 access token
```

| 狀態 | 業務意義 | 可被驗證通過？ | 備註 |
|------|----------|----------------|------|
| `ISSUED`（簽發） | 登入/刷新成功時由 auth-service 以 private key 簽發；`iat` = 簽發時刻，`exp` = 過期時刻 | 是（自 `iat` 起） | 簽發即進入有效 |
| `ACTIVE`（有效） | 在 `iat` ~ `exp` 時間窗內，持 public key 可驗證簽章與時效通過 | 是 | 期間身份與角色以 claims 為準 |
| `EXPIRED`（過期） | 超過 `exp`，時效驗證失敗 | 否 | 需 refresh（Should）或重新登入 |

### 4.2 業務語意問題（AGENTS.md §3 要求回答）

1. **token 能否主動失效（撤銷）？**
   - **否。** JWT 為無狀態，簽發後在 `exp` 前有效，**不可主動撤銷**。blacklist／主動撤銷明確列為 Out of Scope（§7）；「登出」在 POC 中無伺服端撤銷語意，僅以短 TTL 收斂風險（此為 ADR-009 已接受之後果）。

2. **誰能簽發 token？**
   - **僅 auth-service**（持有 RS256 private key）可簽發。其他 service、Gateway、client 均只持有 public key，只能驗證、**無法偽造**。這正是「簽發/驗證隔離」的業務核心（`FR-AUTH-03`）。

3. **使用者看到的是本系統狀態還是 upstream 狀態？**
   - auth-service **不依賴任何 upstream 狀態**：帳號存在性、角色、token 均由本服務自身產生與持有。token 是否被接受，由 Gateway 與各 service 以 public key **獨立驗證**（defense in depth），auth-service 不維護 token 有效性的 runtime 狀態（無狀態）。

4. **哪些角色能改狀態／資料？**
   - `roles`：**無人可經公開 API 修改**；註冊固定 `ROLE_USER`，`ROLE_ADMIN` 以種子資料預先建立（見 UC-4）。
   - `password_hash`：使用者僅能透過「重設密碼」變更——但密碼重設列為 Out of Scope，POC 中密碼**不可變更**（詳見 §7 註記）。
   - token 本身：不可改（無狀態、不可變更）。

5. **哪些欄位不能被使用者直接改？**
   - `sub`（身份）、`roles`（角色）、`exp`/`iat`/`iss`：均由伺服端決定，client 不得指定。
   - `password_hash`：從不進入任何 request/response；client 只提供 password 明文（於註冊/登入的短暫處理中），不得讀取或影響雜湊值。
   - `id`（使用者識別）：系統產生，client 不得指定。

---

## 5. Business Data Dictionary

> 本表定義**欄位的業務意義**（SA 層）。型別、DB type、constraint、index 屬 SD 之 Technical Data Dictionary，**不在本表定義**。

### 5.1 users（使用者帳號）

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `id` | 使用者唯一識別；對應 JWT `sub` | 是（系統產生） | 系統產生之唯一值 | auth-service | internal |
| `username` | 登入帳號名 | 是 | 非空字串；全系統唯一 | 使用者輸入 | personal |
| `email` | 使用者電子郵件 | 是 | 合法 email 格式；全系統唯一 | 使用者輸入 | personal |
| `password_hash` | 密碼之不可逆雜湊（BCrypt） | 是 | BCrypt hash，**絕非明文** | auth-service 產生（對使用者輸入雜湊） | highly sensitive |
| `roles` | 使用者角色集合（授權依據） | 是 | `ROLE_USER` / `ROLE_ADMIN` | auth-service（註冊預設 `ROLE_USER`） | sensitive（授權） |

> **語意註記**：
> - `password`（明文）**不是持久化欄位**：僅存在於註冊/登入請求的短暫處理中，不得持久化、不得記錄、不得回傳；`password_hash` 才是唯一落庫形式（`FR-AUTH-06`）。
> - `roles` 於此以「使用者實體的一個業務欄位」呈現；其 DB 實體歸一化（ADR-002 之 `roles` / `user_roles` 表）屬 SD 層之 Technical Data Dictionary，不在本表定義。

### 5.2 JWT claims 語意（Token 承載的業務資料）

| claim | 業務意義 | 真實來源 | client 可指定？ |
|-------|----------|----------|-----------------|
| `sub` | token 主體 = 使用者識別（userId） | 帳號 `id`（auth-service） | 否 |
| `roles` | 角色清單（授權判定之唯一依據） | 帳號 `roles`（auth-service） | 否 |
| `exp` | 過期時間（token 失效邊界） | 簽發時刻 + TTL（伺服端計算） | 否 |
| `iat` | 簽發時間 | 簽發時刻（伺服端） | 否 |
| `iss` | 簽發者識別（auth-service） | 伺服端常數 | 否 |

> **語意註記**：claims 全數由 auth-service 於簽發時依帳號決定，client 僅提供登入憑證（UC-2）或 refresh token（UC-3），**不得指定任何 claim**。

---

## 6. Acceptance Criteria

> 每條以 GIVEN/WHEN/THEN 描述，標註 AC ID 與對應 FR。

### 6.1 註冊 (Registration)

**AC-AUTH-001 — 註冊成功**（`FR-AUTH-01`, `FR-AUTH-06`）
- GIVEN 提交合法且唯一的 username / email / password
- WHEN GUEST 提交註冊
- THEN 系統建立帳號（預設 `ROLE_USER`）、密碼以 BCrypt 雜湊落庫、回傳結果**不含**密碼或雜湊

**AC-AUTH-002 — 重複註冊被拒**（`FR-AUTH-01`, `FR-X-01`）
- GIVEN username 或 email 已被既有帳號使用
- WHEN GUEST 提交註冊
- THEN 系統回傳 `409`，不建立第二個帳號，錯誤可理解（指明衝突欄位）

**AC-AUTH-003 — 輸入驗證**（`FR-X-01`）
- GIVEN 缺欄位、email 格式非法、或密碼為空
- WHEN GUEST 提交註冊
- THEN 系統回傳驗證錯誤（`400`/`422`），**不落庫**

### 6.2 登入與簽發 (Login & Issuance)

**AC-AUTH-004 — 登入成功簽發 JWT**（`FR-AUTH-02`, `FR-AUTH-03`）
- GIVEN 正確的 username/email 與 password
- WHEN GUEST 提交登入
- THEN 系統簽發 RS256 JWT，claims 含 `sub`/`roles`/`exp`/`iat`/`iss`，且 `sub`、`roles` 正確反映該使用者

**AC-AUTH-005 — 密碼錯誤**（`FR-AUTH-02`, `FR-X-01`）
- GIVEN 帳號存在但密碼錯誤
- WHEN GUEST 提交登入
- THEN 系統回傳 `401`，不簽發 token

**AC-AUTH-006 — 帳號不存在不洩漏**（`FR-X-01`）
- GIVEN username/email 不存在
- WHEN GUEST 提交登入
- THEN 系統回傳 `401`，錯誤訊息與「密碼錯誤」**不可區分**（不洩漏帳號存在性）

### 6.3 簽章隔離 (Signature Isolation)

**AC-AUTH-007 — 簽發/驗證隔離**（`FR-AUTH-03`）
- GIVEN auth-service 簽發的 JWT
- WHEN 以公開的 public key 驗證其簽章
- THEN 簽章驗證通過；且 private key **無法經任何端點取得**，非 auth-service 方**無法偽造** token

**AC-AUTH-008 — public key 公開**（`FR-AUTH-03`）
- GIVEN 未登入（無 token）的請求
- WHEN 取得 public key 公開端點
- THEN 成功取得 public key，供 Gateway 與各 service 驗證（不需 token）

### 6.4 RBAC (Role-Based Access Control)

**AC-AUTH-009 — 越權被拒**（`FR-AUTH-05`, `FR-X-01`）
- GIVEN token 的 `roles` = `ROLE_USER`
- WHEN 存取 admin API（需 `ROLE_ADMIN`）
- THEN 下游 service 回傳 `403 Forbidden`，不執行操作

**AC-AUTH-010 — 授權放行**（`FR-AUTH-05`）
- GIVEN token 的 `roles` = `ROLE_ADMIN`
- WHEN 存取 admin API
- THEN 授權通過，操作正常執行

**AC-AUTH-011 — 角色不可篡改**（`FR-AUTH-05`）
- GIVEN client 於請求中試圖指定或修改 `roles`
- WHEN 系統處理請求
- THEN `roles` 以 token claims（由 auth-service 簽發）為準，client 指定被忽略/拒絕

### 6.5 Token 過期 (Token Expiry)

**AC-AUTH-012 — token 過期**（`FR-AUTH-02`, `FR-X-01`）
- GIVEN token 已超過 `exp`
- WHEN 用於受保護請求
- THEN 回傳 `401`（token 過期），需 refresh（Should）或重新登入

### 6.6 Token 刷新 (Token Refresh — Should)

**AC-AUTH-013 — refresh 成功**（`FR-AUTH-04`）
- GIVEN 有效的 refresh token（未過期）
- WHEN 提交刷新
- THEN 取得新的 access token，`sub`/`roles` 與原 token 一致（身份與權限不變）

**AC-AUTH-014 — refresh 過期/無效**（`FR-AUTH-04`）
- GIVEN 過期或無效的 refresh token
- WHEN 提交刷新
- THEN 回傳 `401`，使用者須重新登入

---

## 7. Out of Scope（本 SA 文件不涵蓋）

| 項目 | 說明 | 來源 |
|------|------|------|
| OAuth2 / OIDC 完整授權流程 | 簡化版「Auth Service 簽發 JWT」即可，完整 Authorization Server 留待演化 | requirements §4 Won't、ADR-009 |
| JWT 主動撤銷（blacklist） | token 到期前不可主動失效；以短 TTL 收斂 | requirements §4 Won't、ADR-009 |
| 密碼重設／忘記密碼 | POC 中密碼**不可變更**；`token_version` 欄位（ADR-009 consequences 之備註）因涉及密碼重設，亦不納入本文件 | FR 清單未列（不擴增） |
| Email 驗證 | 註冊後不寄驗證信、不要求信箱驗證 | FR 清單未列（不擴增） |
| 2FA / MFA 多因素驗證 | 不實作 | FR 清單未列（不擴增） |
| Social login（第三方登入） | 不實作 | FR 清單未列（不擴增） |
| Server-side session 管理 | 採無狀態 JWT，無 server session | ADR-009（否決 session 方案） |
| 帳號鎖定（登入失敗次數鎖定） | 不實作 | FR 清單未列（不擴增） |
| `ROLE_ADMIN` 的授予途徑 | 不提供提權 API／admin 註冊；POC 以**種子資料**預先建立 ADMIN 帳號（見 UC-4） | 最小範圍紀律 |
| API 路由 / 參數 / OpenAPI 3.0 schema | 具體 endpoint、request/response schema、status code 對映表屬 SD（`FR-X-03`, `FR-X-04`） | SD / `docs/api/` |
| DB schema / DDL / DML / ER | 資料表、型別、constraint、index 屬 SD（ADR-002） | SD / `docs/db/` |
| Key 管理與簽章演算法實作 | RS256 key 輪換（JWKS）、Secret Manager 讀取、簽章程式屬 SD（ADR-008/009） | SD |
| 下游授權判定細節 | Gateway 驗證、`X-User-Id`/`X-User-Roles` 轉發、各 service method security 屬 `FR-GW-*` 與下游 service 範圍 | Gateway/各 service spec |

---

## 附錄 A：需求追溯矩陣 (Traceability Matrix)

| FR ID | 需求摘要 | 由 UC 實現 | 由 AC 驗證 |
|-------|----------|-----------|------------|
| FR-AUTH-01 | 使用者註冊（username/email/password） | UC-1 | AC-AUTH-001, AC-AUTH-002, AC-AUTH-003 |
| FR-AUTH-02 | 登入 + 簽發 JWT RS256（sub/roles/exp/iat/iss） | UC-2 | AC-AUTH-004, AC-AUTH-005, AC-AUTH-012 |
| FR-AUTH-03 | 非對稱簽章隔離（private 僅 auth、public 公開） | UC-2 | AC-AUTH-007, AC-AUTH-008 |
| FR-AUTH-04 | Token 刷新（Should） | UC-3 | AC-AUTH-013, AC-AUTH-014 |
| FR-AUTH-05 | RBAC 權限分級（ROLE_USER/ROLE_ADMIN） | UC-4 | AC-AUTH-009, AC-AUTH-010, AC-AUTH-011 |
| FR-AUTH-06 | 密碼不可逆雜湊（BCrypt） | UC-1 | AC-AUTH-001 |
| FR-X-01 | 錯誤流程與輸入驗證（400/401/403/404/409/429/500） | UC-1 ~ UC-4 | AC-AUTH-002, AC-AUTH-003, AC-AUTH-005, AC-AUTH-006, AC-AUTH-009, AC-AUTH-012 |
| FR-GW-06（引用） | 公開端點不需 token（login/register） | UC-1, UC-2（Precondition 語意） | AC-AUTH-008（public key 公開不需 token） |
| NFR-05（引用） | 安全性（JWT RS256、Secret Manager、最小權限） | UC-2, UC-4（語意） | AC-AUTH-007, AC-AUTH-009, AC-AUTH-011 |
