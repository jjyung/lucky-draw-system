# gateway-service — SA 業務需求 (Business Requirements)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SA — Business Requirements（業務行為與語意） |
| **服務範圍** | gateway-service（API Gateway：身份驗證、請求頻率限制、冪等識別檢查、路由、身份傳遞） |

### 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [requirements.md](../requirements.md) | 主需求清單，本文件逐一對齊其 `FR-GW-*`、`FR-X-01` |
| [AGENTS.md](../../../AGENTS.md) | 開發流程指引，本文件遵循其 §3 SA 層模板 |
| [ADR-009](../../adr/009-security-jwt-gateway.md) | 身份驗證 + Gateway 身份傳遞 + 限流 + 公開端點 |
| [ADR-005](../../adr/005-anti-double-draw-idempotency.md) | 冪等識別檢查（抽獎路徑） |
| [ADR-003](../../adr/003-redis-concurrency.md) | 限流計數（依使用者／來源位址） |

> **層級界線**：本文件只定義 gateway 的**業務行為與語意**（use case、business rule、信任邊界語意、acceptance intent、身份傳遞語意）。身份驗證實作、限流計數細節、路由表、API 文件、status code 對映表屬 **SD 層**，不在本文件範圍；本文件引用之 ADR 中的技術細節僅作為業務語意之佐證，不在此重複設計。

---

## 2. 系統／服務定位

### 2.1 Problem & Goal

gateway-service 是系統**唯一的對外流量入口（single entry point）**。所有 client 請求（登入、查活動、抽獎、管理）都經它進入後端微服務。它**不承載業務邏輯**，而是為平台提供一道**可水平擴展、無狀態（stateless）的流量閘道**：在請求抵達業務服務之前，先完成「**驗證 → 限流 → 冪等識別檢查 → 路由 → 身份傳遞**」。

gateway 回答「**系統在邊界上提供什麼存取語意**」：誰可以進來（鑑別）、進多快會被擋（限流）、什麼抽獎請求被視為合法（冪等識別存在性）、請求最後去哪（路由）、下游看到什麼身分（身份傳遞語意）。

### 2.2 Actors & User Roles

| Role | 說明 | 與 gateway 的互動 |
|------|------|----------|
| **匿名訪客 (PUBLIC)** | 未登入的使用者 | 僅能存取公開功能（登入/註冊/活動列表）；訪問受保護功能被拒 `401` |
| **一般使用者 (`ROLE_USER`)** | 已登入的抽獎者 | 經驗證後可存取抽獎功能 |
| **管理員 (`ROLE_ADMIN`)** | 營運人員 | 經驗證後可存取管理功能 |

> gateway 本身**只做鑑別（authentication），不做授權（authorization）**：它驗證憑證真偽與時效，把身份與角色傳遞給下游，由下游服務依角色約束操作範圍（ADR-009 §4）。gateway 不依角色對路由做過濾。

### 2.3 Business Capabilities（本服務提供的能力）

1. 身份驗證（憑證有效性與時效，對所有進入請求）
2. 身份傳遞（將驗證後的身份與角色傳遞給下游，不向下游透傳原始憑證）
3. 限流（使用者層級 + 來源位址層級，超限 `429`）
4. 抽獎路徑的冪等識別存在性檢查（缺則 `400`）
5. 路由轉發（至對應業務服務）
6. 公開功能放行（免登入）

---

## 3. Use Cases

> 每個 use case 依 AGENTS.md §3 格式：Use case name / Actor / Precondition / Main flow / Business rule / Acceptance intent，並帶 **Traceability** 行標註其實現的 `FR-*`。

---

### UC-1 請求驗證與路由 (Request Authentication & Routing)

- **Actor:** PUBLIC / USER / ADMIN（任何 client）
- **Precondition:** 請求進入 gateway（外部入口唯一）。
- **Main flow:**
  1. client 對受保護功能發出請求（需帶身份憑證）。
  2. gateway 驗證憑證有效性與時效。
  3. 驗證失敗（無憑證／憑證無效／過期）→ 回傳 `401 Unauthorized`，不轉發。
  4. 驗證成功 → 解出身份與角色，傳遞給下游，並**不向下游透傳原始憑證**。
  5. gateway 依路由規則將請求轉發至對應業務服務。
- **Business rule:**
  - 驗證範圍為所有請求（公開功能除外，見 UC-4）；有效性與時效**任一不通過即拒**。
  - 原始憑證**不往下游透傳**（降低洩漏風險）；下游只看到已驗證的身份與角色。
  - gateway 只做「鑑別」，**不做「授權」**；角色判定由下游服務依憑證承載的角色執行（ADR-009 §4）。
  - 驗證失敗**不產生任何下游副作用**（請求在邊界即被拒絕）。
- **Acceptance intent:**
  - 有效憑證的請求被正確轉發至對應服務，且下游收到身份與角色、看不到原始憑證。
  - 無憑證、憑證無效或過期 → `401`，不轉發、不觸碰下游。
  - 路由正確：登入/註冊 → auth-service、活動/抽獎 → campaign-service、庫存 → inventory-service。
- **Traceability:** `FR-GW-01`, `FR-GW-02`, `FR-GW-05`, `FR-X-01`

---

### UC-2 限流 (Rate Limiting)

- **Actor:** PUBLIC / USER / ADMIN（任何 client）
- **Precondition:** 請求進入 gateway 的限流判定。
- **Main flow:**
  1. gateway 依使用者層級與來源位址層級，各自獨立進行請求頻率計數。
  2. 任一層級計數超過門檻 → 回傳 `429 Too Many Requests`，不轉發。
  3. 未超限 → 放行，計數於窗口內遞增。
- **Business rule:**
  - 限流維度有二：**使用者層級**（以已驗證之身份）與**來源位址層級**，兩者各自獨立計數、**任一超限即拒**。
  - 超限回傳 `429`，且**不轉發、不觸碰下游**。
  - 計數為**窗口語意**（秒/分級）；窗口長度與門檻值屬 SD/NFR 契約。
  - 限流是「保護平台不被單一 client 打爆」的**邊界控制**，與業務層的「個人抽獎次數上限」（屬 campaign-service，ADR-005）**不同維度、不同語意**，gateway 不分擔後者。
- **Acceptance intent:**
  - 單一使用者（或來源位址）在窗口內超過門檻 → `429`；低於門檻的請求正常放行。
  - 使用者限流與來源位址限流各自生效，互不干擾。
  - 限流不誤傷正常流量。
- **Traceability:** `FR-GW-03`, `FR-X-01`

---

### UC-3 冪等識別檢查（抽獎路徑）

- **Actor:** USER（`ROLE_USER`）
- **Precondition:** 請求為抽獎請求，且已通過身份驗證（UC-1）。
- **Main flow:**
  1. gateway 檢查抽獎請求是否帶有冪等識別（Idempotency-Key）。
  2. 缺少 → 回傳 `400 Bad Request`，不轉發。
  3. 存在 → 放行轉發至 campaign-service。
- **Business rule:**
  - gateway 只做**存在性檢查**，**不驗證格式、不去重**；冪等的真正強制在 campaign-service（ADR-005）。
  - 此檢查是**第一道語法防線**：把「缺冪等識別的抽獎請求」在邊界即擋下，避免進入下游。
  - **只適用於抽獎路徑**；其他請求不要求此識別。
- **Acceptance intent:**
  - 缺冪等識別的抽獎請求 → `400`，不轉發。
  - 帶冪等識別的抽獎請求正常放行，由 campaign-service 執行冪等語意。
  - 非抽獎路徑不因缺此識別而被拒。
- **Traceability:** `FR-GW-04`, `FR-X-01`

---

### UC-4 公開功能放行 (Public Function Pass-through)

- **Actor:** PUBLIC（匿名訪客）
- **Precondition:** 請求命中公開功能。
- **Main flow:**
  1. client 對公開功能（登入、註冊、活動列表）發出請求（無憑證）。
  2. gateway 辨識其為公開功能，**跳過身份驗證**。
  3. gateway 依路由規則轉發至對應服務（登入/註冊 → auth-service；活動列表 → campaign-service）。
- **Business rule:**
  - 公開功能清單**封閉**：僅登入、註冊、活動列表免憑證；其餘請求一律需憑證。
  - 公開功能**仍受限流保護**（UC-2），但**不受 UC-1 的身份驗證**。
  - 公開功能轉發時**無**身份與角色可傳遞。
- **Acceptance intent:**
  - 無憑證存取上述公開功能成功放行（不含管理欄位）。
  - 無憑證存取**非**公開功能 → `401`。
  - 公開功能清單外的任何路徑不會被誤判為免憑證。
- **Traceability:** `FR-GW-06`

---

## 4. Business State

### 4.1 gateway 為無狀態服務

gateway **不持有任何業務狀態**（無 domain entity、無業務資料表）。它是純粹的流量閘道：每個請求獨立處理，不記憶前一請求。唯二與「狀態」沾邊者：

- **限流計數**（窗口內遞增、窗口外過期）——屬**邊界控制狀態**，非業務狀態；
- **身份驗證**——無狀態，憑證自帶身分與時效，gateway 不存 session。

因此 gateway **可水平擴展**：任何實例做同一件事，無 session 同步成本（ADR-009 consequence、NFR-02）。

### 4.2 信任邊界語意 (Trust Boundary Semantics)

gateway 是**第一道信任邊界**，但**不是唯一一道**。其傳遞的身份與角色資訊的語意是：

> **「gateway 已驗證此憑證」的附加資訊，而非下游身分的權威來源。**

**為何 gateway 不能只驗證一次、就讓下游信任傳遞的身份？**（AGENTS.md §3 語意問題）

1. **身份資訊可偽造**：傳遞的身份/角色是普通請求欄位，無加密簽章。任何能直達下游服務的呼叫者（內網直連、路由誤設定）都能自行塞入偽裝身分。傳遞欄位是「可被欺騙的」，唯憑證簽章是「不可偽造的」。
2. **防禦縱深（Defense in Depth）**：若 gateway 的驗證有 bug、被繞過（路由誤設定）、或某服務意外被直接暴露，下游若只信傳遞欄位，就等於毫無保護。下游**獨立複驗憑證**使每一層都能自保，單層失誤不成為整體破口（ADR-009 §3）。
3. **單點信任假設過強**：把「gateway 驗證過」當作硬安全邊界，等於把全系統安全押在 gateway 一層的正確性與可用性上；gateway 一旦有誤，全下游失守。獨立複驗把「驗證」分散到每個服務，縮小單點風險。

**語意結論**：下游服務把傳遞的身份/角色視為「Gateway 已驗證」的**提示（supplementary）**，權威身分以其**自行複驗的憑證為準**。此即 ADR-009 的「多層防禦」語意——gateway 的驗證是「加速與第一道攔截」，非「唯一信任源」。

> **開放問題（Open Point，屬 SD 決策）**：ADR-009 §3 要求下游服務獨立複驗憑證，但 FR-GW-02 要求 gateway 不向下游透傳原始憑證。兩者之間「下游如何取得憑證以複驗」的傳遞機制**屬 SD 層技術決策**，本 SA 文件僅點明該語意張力，不在此設計。SA 語意結論不變：**權威身分 = 憑證自行複驗，傳遞欄位 = 提示**。

### 4.3 語意問題回應（AGENTS.md §3）

1. **狀態能否回轉？** — gateway 無業務狀態，無回轉問題；限流計數窗口過期即自然重置。
2. **使用者看到的是本系統狀態還是 upstream 狀態？** — gateway 不向使用者呈現任何業務狀態；它傳遞的「身分」是從憑證（auth-service 簽發的 **upstream 憑證**）解出的，gateway 本身不產生、不儲存身分。
3. **哪些角色能改狀態？** — 無狀態可改；gateway 不提供任何狀態變更能力。
4. **哪些欄位不能被使用者直接改？** — 身份與角色由 gateway 依**驗證結果**產生，client 不可自行指定（gateway 以驗證結果覆寫 client 自帶的同名欄位，見 §5）；原始憑證由 gateway 移除，不向下游透傳。

---

## 5. Business Data Dictionary

> gateway 無 domain entity 與業務資料表；本表僅定義**傳遞欄位的業務語意**（SA 層）。欄位的實際名稱、型別、處理時機屬 SD 之 Technical Data Dictionary，**不在本表定義**。

### 5.1 傳遞身份欄位

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| 使用者識別 | 已驗證使用者之識別（對應憑證承載的身份） | 是（受保護功能） | 有效使用者識別 | 憑證（auth-service 簽發），gateway 解出後傳遞 | personal |
| 角色清單 | 已驗證使用者之角色（對應憑證承載的角色） | 是（受保護功能） | `ROLE_USER` / `ROLE_ADMIN`（可多值） | 憑證（auth-service 簽發），gateway 解出後傳遞 | internal |
| 原始憑證 | client 提供的憑證 | 是（受保護功能之輸入） | 有效憑證 | client 提供 | sensitive（credential） |

> **語意註記**：
> - 使用者識別/角色由 gateway **依驗證結果產生**，client **不得自行指定**；gateway 傳遞前須以驗證結果**覆寫** client 自帶的同名欄位（若有的話），避免偽造（`inferred`，confidence high——源於 ADR-009 defense-in-depth 語意）。
> - 原始憑證由 gateway **移除**，不向下游透傳；下游不應、也不需要看到原始憑證（降低洩漏面）。
> - 公開功能（UC-4）無憑證可解，傳遞時**不帶**使用者識別/角色。
> - 這些欄位的語意是「Gateway 已驗證」的**提示**，非下游身分權威來源（見 §4.2）。

---

## 6. Acceptance Criteria

> 每條以 GIVEN/WHEN/THEN 描述，標註 AC ID 與對應 FR。

### 6.1 驗證與路由 (Authentication & Routing)

**AC-GW-001 — 無憑證訪問受保護功能 → 401**（`FR-GW-01`, `FR-X-01`）
- GIVEN 一個未帶憑證的請求
- WHEN 該請求命中受保護功能（非公開功能）
- THEN gateway 回傳 `401 Unauthorized`，且不轉發至下游服務

**AC-GW-002 — 憑證無效 → 401**（`FR-GW-01`, `FR-X-01`）
- GIVEN 一個帶憑證但無效（被竄改或非 auth-service 簽發）的請求
- WHEN gateway 驗證
- THEN gateway 回傳 `401`，不轉發

**AC-GW-003 — 憑證過期 → 401**（`FR-GW-01`, `FR-X-01`）
- GIVEN 一個有效但已過期的憑證
- WHEN gateway 檢查時效
- THEN gateway 回傳 `401`，不轉發

**AC-GW-004 — 有效憑證正確轉發並傳遞身份**（`FR-GW-01`, `FR-GW-02`, `FR-GW-05`）
- GIVEN 一個帶有效憑證的受保護請求
- WHEN gateway 驗證成功
- THEN 請求被轉發至正確的下游服務，且下游收到身份與角色，且**看不到**原始憑證

**AC-GW-005 — 路由正確性**（`FR-GW-05`）
- GIVEN 各類請求（登入/註冊 / 活動/抽獎 / 庫存）
- WHEN gateway 依路由規則轉發
- THEN 登入/註冊 → auth-service、活動/抽獎 → campaign-service、庫存 → inventory-service

### 6.2 限流 (Rate Limiting)

**AC-GW-006 — 超限 → 429**（`FR-GW-03`, `FR-X-01`）
- GIVEN 單一使用者（或單一來源位址）在限流窗口內發出超過門檻的請求
- WHEN gateway 判定超限
- THEN gateway 回傳 `429 Too Many Requests`，且不轉發、不觸碰下游

**AC-GW-007 — 使用者與來源位址限流獨立生效**（`FR-GW-03`）
- GIVEN 同一位址下有多個不同使用者，或同一使用者從多個位址發出請求
- WHEN gateway 分別依使用者維度與來源位址維度計數
- THEN 兩維度各自獨立判定，任一超限即 `429`，不互相干擾

### 6.3 冪等識別檢查 (Idempotency-Key Check)

**AC-GW-008 — 抽獎請求缺冪等識別 → 400**（`FR-GW-04`, `FR-X-01`）
- GIVEN 一個已通過身份驗證的抽獎請求
- WHEN 該請求未帶冪等識別（Idempotency-Key）
- THEN gateway 回傳 `400 Bad Request`，且不轉發至 campaign-service

**AC-GW-009 — 帶冪等識別的抽獎請求放行**（`FR-GW-04`）
- GIVEN 抽獎請求帶有冪等識別
- WHEN gateway 檢查存在性
- THEN 請求放行轉發至 campaign-service（冪等語意由下游執行）

### 6.4 公開功能 (Public Functions)

**AC-GW-010 — 公開功能免憑證放行**（`FR-GW-06`）
- GIVEN 無憑證的請求命中登入、註冊或活動列表
- WHEN gateway 辨識為公開功能
- THEN 請求跳過身份驗證，成功轉發至對應服務

**AC-GW-011 — 公開功能清單封閉**（`FR-GW-06`, `FR-X-01`）
- GIVEN 無憑證的請求命中**非**公開功能
- WHEN gateway 驗證
- THEN 回傳 `401`（與 AC-GW-001 一致），不會被誤判為免憑證

---

## 7. Out of Scope（本 SA 文件不涵蓋）

| 項目 | 說明 | 後續層 |
|------|------|--------|
| 身份驗證實作 / 憑證輪替 / 安全過濾鏈 | 屬 SD（ADR-009） | SD |
| 限流計數細節 / 窗口演算法 / 門檻值 | 屬 SD / NFR 契約（ADR-003） | SD |
| 路由表（路由規則 / 過濾器） | 屬 SD（ADR-001, ADR-009） | SD |
| API 文件 / status code 對映表 / 錯誤 response schema | 屬 SD（FR-X-03, FR-X-04） | SD / `docs/api/` |
| 授權判定 | 屬下游服務（ADR-009 §4），gateway 僅鑑別 | Auth/Campaign spec |
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
| FR-GW-01 | 身份驗證（憑證有效性與時效） | UC-1 | AC-GW-001, AC-GW-002, AC-GW-003, AC-GW-004 |
| FR-GW-02 | 身份傳遞（身份/角色傳遞，不向下游透傳原始憑證） | UC-1 | AC-GW-004 |
| FR-GW-03 | 使用者/來源位址限流，超限 429 | UC-2 | AC-GW-006, AC-GW-007 |
| FR-GW-04 | 抽獎請求冪等識別檢查，缺則 400 | UC-3 | AC-GW-008, AC-GW-009 |
| FR-GW-05 | 路由轉發至對應業務服務 | UC-1 | AC-GW-004, AC-GW-005 |
| FR-GW-06 | 公開功能免憑證 | UC-4 | AC-GW-010, AC-GW-011 |
| FR-X-01 | 錯誤流程（401/429/400 語意） | UC-1, UC-2, UC-3, UC-4 | AC-GW-001/002/003/006/008/011 |
