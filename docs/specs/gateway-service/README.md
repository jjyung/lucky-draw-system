# gateway-service — SA 業務需求 (Business Requirements)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SA — Business Requirements（業務行為與語意） |
| **服務範圍** | gateway-service（API Gateway：JWT 驗證、限流、Idempotency-Key 檢查、路由、header 轉發） |

### 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [requirements.md](../requirements.md) | 主需求清單，本文件逐一對齊其 `FR-GW-*`、`FR-X-01` |
| [AGENTS.md](../../../AGENTS.md) | 開發流程指引，本文件遵循其 §3 SA 層模板 |
| [ADR-009](../../adr/009-security-jwt-gateway.md) | JWT（RS256）驗證 + Gateway header 轉發 + 限流 + 公開端點 |
| [ADR-005](../../adr/005-anti-double-draw-idempotency.md) | Idempotency-Key 檢查（draw 路徑） |
| [ADR-003](../../adr/003-redis-concurrency.md) | 限流 Redis 計數器（`rate:{userId}` / `rate:{clientIp}`） |

> **層級界線**：本文件只定義 gateway 的**業務行為與語意**（use case、business rule、信任邊界語意、acceptance intent、轉發 header 語意）。JWT 驗證實作（JWKS 輪替、Security filter chain）、Redis 限流 key/窗口/門檻、路由表（route predicate/filter）、OpenAPI schema、status code 對映表屬 **SD 層**，不在本文件範圍；本文件引用之 ADR 中的技術細節僅作為業務語意之佐證，不在此重複設計。

---

## 2. 系統／服務定位

### 2.1 Problem & Goal

gateway-service 是系統**唯一的對外流量入口（single entry point）**。所有 client 請求（登入、查活動、抽獎、管理）都經它進入後端微服務。它**不承載業務邏輯**，而是為平台提供一道**可水平擴展、無狀態（stateless）的流量閘道**：在請求抵達業務服務之前，先完成「**驗證 → 限流 → Idempotency-Key 檢查 → 路由 → header 轉發**」。

gateway 回答「**系統在邊界上提供什麼存取語意**」：誰可以進來（鑑別）、進多快會被擋（限流）、什麼 draw 請求被視為合法（冪等鍵存在性）、請求最後去哪（路由）、下游看到什麼身分（header 轉發語意）。

### 2.2 Actors & User Roles

| Role | 說明 | 與 gateway 的互動 |
|------|------|----------|
| **匿名訪客 (PUBLIC)** | 未登入的使用者 | 僅能存取公開端點（登入/註冊/活動列表）；訪問受保護端點被拒 `401` |
| **一般使用者 (`ROLE_USER`)** | 已登入的抽獎者 | 經 JWT 驗證後可存取抽獎與個人記錄端點 |
| **管理員 (`ROLE_ADMIN`)** | 營運人員 | 經 JWT 驗證後可存取管理端點 |

> gateway 本身**只做鑑別（authentication），不做授權（authorization / RBAC）**：它驗證 token 真偽與時效，把身分 claim 轉發給下游，由下游 service 依角色約束操作範圍（ADR-009 §4）。gateway 不依 `roles` 對路由做角色過濾。

### 2.3 Business Capabilities（本服務提供的能力）

1. JWT 驗證（RS256 簽章 + 過期，對所有 `/api/**`）
2. 身分 claim 轉發（`X-User-Id` / `X-User-Roles`）與 `Authorization` 移除
3. 限流（user 層級 + IP 層級，Redis 計數器，超限 `429`）
4. draw 路徑的 Idempotency-Key 存在性檢查（缺則 `400`）
5. 路由轉發（auth-service / campaign-service / inventory-service）
6. 公開端點放行（免 token）

---

## 3. Use Cases

> 每個 use case 依 AGENTS.md §3 格式：Use case name / Actor / Precondition / Main flow / Business rule / Acceptance intent，並帶 **Traceability** 行標註其實現的 `FR-*`。

---

### UC-1 請求驗證與路由 (Request Authentication & Routing)

- **Actor:** PUBLIC / USER / ADMIN（任何呼叫 `/api/**` 的 client）
- **Precondition:** 請求進入 gateway（外部入口唯一）。
- **Main flow:**
  1. client 對 `/api/**` 發出請求（受保護端點需帶 `Authorization: Bearer <JWT>`）。
  2. gateway 以 RS256 public key 驗證 JWT 簽章，並檢查 `exp` 是否過期。
  3. 驗證失敗（無 token／簽章無效／過期）→ 回傳 `401 Unauthorized`，不轉發。
  4. 驗證成功 → 解出 claims，將 `sub` → `X-User-Id`、`roles` → `X-User-Roles` 寫入轉發 header，並**移除原始 `Authorization` header**。
  5. gateway 依路由規則將請求轉發至 auth-service / campaign-service / inventory-service。
- **Business rule:**
  - 驗證範圍為所有 `/api/**`（公開端點除外，見 UC-4）；簽章與過期**任一不通過即拒**。
  - `Authorization` header **不往下游透傳**（降低下游洩漏風險）；下游只看到 `X-User-Id` / `X-User-Roles` 與原有業務 header。
  - gateway 只做「鑑別」，**不做「授權」**；角色判定由下游 service 依 claims/`X-User-Roles` 執行（ADR-009 §4）。
  - 驗證失敗**不產生任何下游副作用**（請求在邊界即被拒絕）。
- **Acceptance intent:**
  - 有效 token 的請求被正確轉發至對應 service，且下游收到 `X-User-Id` / `X-User-Roles`、看不到 `Authorization`。
  - 無 token、簽章無效或過期 → `401`，不轉發、不觸碰下游。
  - 路由正確：auth 路徑 → auth-service、campaign/draw 路徑 → campaign-service、庫存路徑 → inventory-service。
- **Traceability:** `FR-GW-01`, `FR-GW-02`, `FR-GW-05`, `FR-X-01`

---

### UC-2 Rate Limiting (user/IP 層級限流)

- **Actor:** PUBLIC / USER / ADMIN（任何 client）
- **Precondition:** 請求進入 gateway 的限流判定。
- **Main flow:**
  1. gateway 以 Redis 計數器對請求進行限流判定（user 層級 key 與 IP 層級 key **各自獨立**）。
  2. 任一層級計數超過門檻 → 回傳 `429 Too Many Requests`，不轉發。
  3. 未超限 → 放行，計數器於窗口內遞增。
- **Business rule:**
  - 限流維度有二：**user 層級**（以已驗證之 `userId`）與 **IP 層級**（以 client 來源 IP），兩者各自獨立計數、**任一超限即拒**。
  - 超限回傳 `429`，且**不轉發、不觸碰下游**。
  - 計數器為**窗口語意**（秒/分級滑動窗口），由 Redis 承擔（ADR-003 `rate:{userId}` / `rate:{clientIp}`）；窗口長度與門檻值屬 SD/NFR 契約。
  - 限流是「保護平台不被單一 client 打爆」的**邊界控制**，與業務層的「個人抽獎次數上限」（`draw_count`，屬 campaign-service，ADR-005）**不同維度、不同語意**，gateway 不分擔後者。
- **Acceptance intent:**
  - 單一 user（或 IP）在窗口內超過門檻 → `429`；低於門檻的請求正常放行。
  - user 限流與 IP 限流各自生效，互不干擾。
  - 限流不誤傷正常流量。
- **Traceability:** `FR-GW-03`, `FR-X-01`

---

### UC-3 Idempotency-Key 檢查（draw 路徑）

- **Actor:** USER（`ROLE_USER`）
- **Precondition:** 請求為 `POST /campaigns/{id}/draw`，且已通過 JWT 驗證（UC-1）。
- **Main flow:**
  1. gateway 檢查 draw 請求是否帶有 `Idempotency-Key` header。
  2. 缺少 → 回傳 `400 Bad Request`，不轉發。
  3. 存在 → 放行轉發至 campaign-service。
- **Business rule:**
  - gateway 只做**存在性檢查**（header 有無），**不驗證值是否為 UUID、不去重、不查 Redis 鎖**；冪等的真正強制在 campaign-service（複合冪等鍵 `userId + campaignId + idempotencyKey` + 兩道防線，ADR-005）。
  - 此檢查是**第一道語法防線**：把「缺 key 的 draw 請求」在邊界即擋下，避免進入下游。
  - **只適用於 draw 路徑**（`POST /campaigns/{id}/draw`）；其他請求不要求此 header。
- **Acceptance intent:**
  - 缺 `Idempotency-Key` 的 draw 請求 → `400`，不轉發。
  - 帶 key 的 draw 請求正常放行，由 campaign-service 執行冪等語意。
  - 非 draw 路徑不因缺此 header 而被拒。
- **Traceability:** `FR-GW-04`, `FR-X-01`

---

### UC-4 公開端點放行 (Public Endpoint Pass-through)

- **Actor:** PUBLIC（匿名訪客）
- **Precondition:** 請求命中公開端點。
- **Main flow:**
  1. client 對公開端點（`POST /auth/login`、`POST /auth/register`、`GET /campaigns`）發出請求（無 token）。
  2. gateway 辨識其為公開端點，**跳過 JWT 驗證**。
  3. gateway 依路由規則轉發至對應 service（login/register → auth-service；campaign 列表 → campaign-service）。
- **Business rule:**
  - 公開端點清單**封閉**：僅 `POST /auth/login`、`POST /auth/register`、`GET /campaigns` 免 token；其餘 `/api/**` 一律需 token。
  - 公開端點**仍受限流保護**（UC-2），但**不受 UC-1 的 JWT 驗證**。
  - 公開端點轉發時**無** `X-User-Id` / `X-User-Roles`（無 claims 可解）。
- **Acceptance intent:**
  - 無 token 存取上述公開端點成功放行（不含管理欄位）。
  - 無 token 存取**非**公開端點 → `401`。
  - 公開端點清單外的任何路徑不會被誤判為免 token。
- **Traceability:** `FR-GW-06`

---

## 4. Business State

### 4.1 gateway 為無狀態服務

gateway **不持有任何業務狀態**（無 domain entity、無 DB 業務表）。它是純粹的流量閘道：每個請求獨立處理，不記憶前一請求。唯二與「狀態」沾邊者：

- **限流計數器**（Redis，window 內遞增、window 外過期）——屬**邊界控制狀態**，非業務狀態；
- **JWT 驗證**——stateless，token 自帶身分與時效，gateway 不存 session。

因此 gateway **可水平擴展**：任何 instance 做同一件事，無 session 同步成本（ADR-009 consequence、NFR-02）。

### 4.2 信任邊界語意 (Trust Boundary Semantics)

gateway 是**第一道信任邊界**，但**不是唯一一道**。其轉發的 `X-User-Id` / `X-User-Roles` header 的語意是：

> **「gateway 已驗證此 token」的附加資訊，而非下游身分的權威來源。**

**為何 gateway 不能只驗證一次、就讓下游信任轉發的 header？**（AGENTS.md §3 語意問題）

1. **header 可偽造**：`X-User-Id` / `X-User-Roles` 是普通 HTTP header，無加密簽章。任何能直達下游 service 的呼叫者（內網直連、路由誤設定）都能自行塞入 `X-User-Id: <任意 id>`、`X-User-Roles: ROLE_ADMIN` 偽裝身分。header 是「可被欺騙的」，唯 JWT 簽章（RS256 public key 可驗）是「不可偽造的」。
2. **防禦縱深（Defense in Depth）**：若 gateway 的驗證 filter 有 bug、被繞過（misconfigured route）、或某 service 意外被直接暴露，下游若只信 header，就等於毫無保護。下游**獨立複驗 JWT 簽章**使每一層都能自保，單層失誤不成為整體破口（ADR-009 §3）。
3. **單點信任假設過強**：把「gateway 驗證過」當作硬安全邊界，等於把全系統安全押在 gateway 一層的正確性與可用性上；gateway 一旦有誤，全下游失守。獨立複驗把「驗證」分散到每個 service，縮小單點風險。

**語意結論**：下游 service 把 `X-User-Id` / `X-User-Roles` 視為「Gateway 已驗證」的**提示（supplementary）**，權威身分以其**自行複驗的 JWT claims 為準**。此即 ADR-009 的「多層防禦」語意——gateway 的驗證是「加速與第一道攔截」，非「唯一信任源」。

> **開放問題（Open Point，屬 SD 決策）**：ADR-009 §3 要求下游 service 獨立複驗 JWT 簽章，但 FR-GW-02 要求 gateway 移除原始 `Authorization` header。兩者之間「下游如何取得 token 以複驗」的傳遞機制（例如 gateway 以受控方式重傳 token、或下游另有可信內部通道）**屬 SD 層技術決策**，本 SA 文件僅點明該語意張力，不在此設計。SA 語意結論不變：**權威身分 = JWT 自行複驗，轉發 header = 提示**。

### 4.3 語意問題回應（AGENTS.md §3）

1. **狀態能否回轉？** — gateway 無業務狀態，無回轉問題；限流計數器 window 過期即自然重置。
2. **使用者看到的是本系統狀態還是 upstream 狀態？** — gateway 不向使用者呈現任何業務狀態；它轉發的「身分」是從 token（auth-service 簽發的 **upstream 憑證**）解出的 claims，gateway 本身不產生、不儲存身分。
3. **哪些角色能改狀態？** — 無狀態可改；gateway 不提供任何狀態變更能力。
4. **哪些欄位不能被使用者直接改？** — `X-User-Id` / `X-User-Roles` 由 gateway 依**驗證後的 claims** 產生，client 不可自行指定（gateway 以驗證結果覆寫 client 自帶的同名 header，見 §5）；`Authorization` 由 gateway 移除，不向下游透傳。

---

## 5. Business Data Dictionary

> gateway 無 domain entity 與 DB 業務表；本表僅定義**轉發 header 的業務語意**（SA 層）。header 的實際名稱、型別、HTTP 層處理時機（移除/覆寫）屬 SD 之 Technical Data Dictionary，**不在本表定義**。

### 5.1 轉發身份 header

| 欄位 (header) | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `X-User-Id` | 已驗證使用者之識別（對應 JWT `sub`） | 是（受保護端點） | 有效使用者識別 | JWT claims（auth-service 簽發），gateway 解出後轉發 | personal |
| `X-User-Roles` | 已驗證使用者之角色清單（對應 JWT `roles`） | 是（受保護端點） | `ROLE_USER` / `ROLE_ADMIN`（可多值） | JWT claims（auth-service 簽發），gateway 解出後轉發 | internal |
| `Authorization` | 原始憑證（`Bearer <RS256 JWT>`） | 是（受保護端點之輸入） | `Bearer <RS256 JWT>` | client 提供 | sensitive（credential） |

> **語意註記**：
> - `X-User-Id` / `X-User-Roles` 由 gateway **依驗證後的 claims 產生**，client **不得自行指定**；gateway 轉發前須以驗證結果**覆寫** client 自帶的同名 header（若有的話），避免 header 偽造（`inferred`，confidence high——源於 ADR-009 defense-in-depth 語意）。
> - `Authorization` 由 gateway **移除**，不向下游透傳；下游不應、也不需要看到原始憑證（降低洩漏面）。
> - 公開端點（UC-4）無 claims 可解，轉發時**不帶** `X-User-Id` / `X-User-Roles`。
> - 這些 header 的語意是「Gateway 已驗證」的**提示**，非下游身分權威來源（見 §4.2）。

---

## 6. Acceptance Criteria

> 每條以 GIVEN/WHEN/THEN 描述，標註 AC ID 與對應 FR。

### 6.1 驗證與路由 (Authentication & Routing)

**AC-GW-001 — 無 token 訪問受保護端點 → 401**（`FR-GW-01`, `FR-X-01`）
- GIVEN 一個未帶 `Authorization` header 的請求
- WHEN 該請求命中受保護端點（非公開端點）
- THEN gateway 回傳 `401 Unauthorized`，且不轉發至下游 service

**AC-GW-002 — 簽章無效 token → 401**（`FR-GW-01`, `FR-X-01`）
- GIVEN 一個帶 JWT 但簽章無效（被竄改或非 auth-service 私鑰簽署）的請求
- WHEN gateway 以 RS256 public key 驗證
- THEN gateway 回傳 `401`，不轉發

**AC-GW-003 — 過期 token → 401**（`FR-GW-01`, `FR-X-01`）
- GIVEN 一個帶有效簽章但 `exp` 已過期的 JWT
- WHEN gateway 檢查過期時間
- THEN gateway 回傳 `401`，不轉發

**AC-GW-004 — 有效 token 正確轉發並注入 header**（`FR-GW-01`, `FR-GW-02`, `FR-GW-05`）
- GIVEN 一個帶有效 JWT（`sub`/`roles`/`exp` 合法）的受保護請求
- WHEN gateway 驗證成功
- THEN 請求被轉發至正確的下游 service，且下游收到 `X-User-Id`（= `sub`）、`X-User-Roles`（= `roles`），且**看不到**原始 `Authorization` header

**AC-GW-005 — 路由正確性**（`FR-GW-05`）
- GIVEN 各類請求（auth / campaign / inventory 路徑）
- WHEN gateway 依路由規則轉發
- THEN auth 路徑 → auth-service、campaign/draw 路徑 → campaign-service、inventory 路徑 → inventory-service

### 6.2 限流 (Rate Limiting)

**AC-GW-006 — 超限 → 429**（`FR-GW-03`, `FR-X-01`）
- GIVEN 單一 user（或單一 IP）在限流窗口內發出超過門檻的請求
- WHEN gateway 以 Redis 計數器判定
- THEN gateway 回傳 `429 Too Many Requests`，且不轉發、不觸碰下游

**AC-GW-007 — user 與 IP 限流獨立生效**（`FR-GW-03`）
- GIVEN 同一 IP 下有多個不同 user，或同一 user 從多個 IP 發出請求
- WHEN gateway 分別依 user 維度與 IP 維度計數
- THEN 兩維度各自獨立判定，任一超限即 `429`，不互相干擾

### 6.3 Idempotency-Key 檢查 (Idempotency-Key Check)

**AC-GW-008 — draw 請求缺 Idempotency-Key → 400**（`FR-GW-04`, `FR-X-01`）
- GIVEN 一個已通過 JWT 驗證的 `POST /campaigns/{id}/draw` 請求
- WHEN 該請求未帶 `Idempotency-Key` header
- THEN gateway 回傳 `400 Bad Request`，且不轉發至 campaign-service

**AC-GW-009 — 帶 Idempotency-Key 的 draw 請求放行**（`FR-GW-04`）
- GIVEN draw 請求帶有 `Idempotency-Key` header
- WHEN gateway 檢查存在性
- THEN 請求放行轉發至 campaign-service（冪等語意由下游執行）

### 6.4 公開端點 (Public Endpoints)

**AC-GW-010 — 公開端點免 token 放行**（`FR-GW-06`）
- GIVEN 無 token 的請求命中 `POST /auth/login`、`POST /auth/register` 或 `GET /campaigns`
- WHEN gateway 辨識為公開端點
- THEN 請求跳過 JWT 驗證，成功轉發至對應 service

**AC-GW-011 — 公開端點清單封閉**（`FR-GW-06`, `FR-X-01`）
- GIVEN 無 token 的請求命中**非**公開端點
- WHEN gateway 驗證
- THEN 回傳 `401`（與 AC-GW-001 一致），不會被誤判為免 token

---

## 7. Out of Scope（本 SA 文件不涵蓋）

| 項目 | 說明 | 後續層 |
|------|------|--------|
| JWT 驗證實作 / JWKS 輪替 / Security filter chain | 屬 SD（ADR-009） | SD |
| Redis 限流 key schema / 窗口演算法 / 門檻值 | 屬 SD / NFR 契約（ADR-003） | SD |
| 路由表（route predicate / filter / rewrite） | 屬 SD（ADR-001, ADR-009） | SD |
| OpenAPI 3.0 / status code 對映表 / 錯誤 response schema | 屬 SD（FR-X-03, FR-X-04） | SD / `docs/api/` |
| 授權（RBAC）判定 | 屬下游 service（ADR-009 §4），gateway 僅鑑別 | Auth/Campaign spec |
| 冪等的去重 / replay 語意 | 屬 campaign-service（ADR-005），gateway 僅存在性檢查 | campaign-service spec |
| 金絲雀 / 藍綠部署路由 | 天條最小需求，不做 | — |
| request 轉寫（payload rewrite） | 不做 | — |
| 響應快取（response caching） | 不做 | — |
| 熔斷器（circuit breaker） | 不做 | — |
| retry / backoff 政策 | 不做 | — |
| API 版本管理 | 不做 | — |
| WebSocket 代理 | 不做 | — |
| CORS / CSRF 細節 | 不做 | — |
| 請求簽章（request signing） | 不做 | — |
| IP 白名單 | 不做 | — |

---

## 附錄 A：需求追溯矩陣 (Traceability Matrix)

| FR ID | 需求摘要 | 由 UC 實現 | 由 AC 驗證 |
|-------|----------|-----------|------------|
| FR-GW-01 | `/api/**` JWT 簽章（RS256）與過期驗證 | UC-1 | AC-GW-001, AC-GW-002, AC-GW-003, AC-GW-004 |
| FR-GW-02 | claims 轉發 `X-User-Id`/`X-User-Roles`，移除 `Authorization` | UC-1 | AC-GW-004 |
| FR-GW-03 | user/IP 層級限流（Redis 計數器），超限 429 | UC-2 | AC-GW-006, AC-GW-007 |
| FR-GW-04 | draw 請求 Idempotency-Key 檢查，缺則 400 | UC-3 | AC-GW-008, AC-GW-009 |
| FR-GW-05 | 路由轉發至 auth/campaign/inventory | UC-1 | AC-GW-004, AC-GW-005 |
| FR-GW-06 | 公開端點免 token | UC-4 | AC-GW-010, AC-GW-011 |
| FR-X-01 | 錯誤流程（401/429/400 語意） | UC-1, UC-2, UC-3, UC-4 | AC-GW-001/002/003/006/008/011 |
