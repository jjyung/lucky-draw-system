# gateway-service — 使用情境 (User Stories)

> 對應 SA 規格：[gateway-service/README.md](../specs/gateway-service/README.md)。天條依據見 [requirements.md](../specs/requirements.md) §0。
>
> **定位**：gateway 是唯一對外入口，無業務狀態、只做「驗證 → 限流 → 冪等識別檢查 → 路由 → 身份傳遞」，不承載業務邏輯。其 story 的角色為「平台邊界」。

---

### ST-GW-001 — 身份驗證（憑證有效性與時效）

- **User Story:** As a 平台邊界, I want to 驗證所有進入請求的身份憑證有效性與時效、未通過者拒絕, so that 未驗證請求無法觸及後端服務。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-GW-01`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」
- **對應規格:** UC-1（gateway-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 無憑證訪問受保護功能，WHEN 請求進入，THEN 回傳 `401`、不轉發（AC-GW-001）。
  - GIVEN 憑證無效（被竄改/非簽發方），WHEN 驗證，THEN 回傳 `401`（AC-GW-002）。
  - GIVEN 憑證已過期，WHEN 驗證，THEN 回傳 `401`（AC-GW-003）。

---

### ST-GW-002 — 身份傳遞給下游

- **User Story:** As a 平台邊界, I want to 將驗證後的身份與角色傳遞給下游服務（不透傳原始憑證）, so that 下游能依角色授權且憑證洩漏面最小。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-GW-02`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」
- **對應規格:** UC-1（gateway-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 有效憑證的受保護請求，WHEN 驗證成功，THEN 轉發至正確下游、下游收到身份與角色、看不到原始憑證（AC-GW-004）。

---

### ST-GW-003 — 請求頻率限制

- **User Story:** As a 平台邊界, I want to 限制單一使用者與來源位址的請求頻率、超限回傳 `429`, so that 平台不被單一 client 打爆。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-GW-03`
- **天條依據 (Source):** §0「完整的錯誤流程處理」（限流為邊界保護；屬 ADR-003/009 細化）
- **對應規格:** UC-2（gateway-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 單一使用者（或來源位址）在窗口內超門檻，WHEN 判定，THEN 回傳 `429`、不轉發（AC-GW-006）。
  - GIVEN 使用者維度與來源位址維度，WHEN 分別計數，THEN 各自獨立、任一超限即拒、互不干擾（AC-GW-007）。

---

### ST-GW-004 — 抽獎請求冪等識別檢查

- **User Story:** As a 平台邊界, I want to 要求抽獎請求帶冪等識別（Idempotency-Key）、缺少時回傳 `400`, so that 缺冪等識別的抽獎請求在邊界即被擋下。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-GW-04`
- **天條依據 (Source):** §0「防止重複抽獎」（冪等識別為防重複抽獎的第一道語法防線）
- **對應規格:** UC-3（gateway-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 已驗證的抽獎請求缺冪等識別，WHEN 檢查，THEN 回傳 `400`、不轉發（AC-GW-008）。
  - GIVEN 抽獎請求帶冪等識別，WHEN 檢查，THEN 放行（冪等語意由下游執行）（AC-GW-009）。

---

### ST-GW-005 — 請求路由

- **User Story:** As a 平台邊界, I want to 作為統一入口將請求路由至對應業務服務, so that 各業務服務職責清晰且 client 只需面對單一入口。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-GW-05`
- **天條依據 (Source):** §0「前後端分離，後端採用 RESTful API 風格，並提供清晰的路由與參數說明」
- **對應規格:** UC-1（gateway-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 各類請求，WHEN 依路由規則轉發，THEN 登入/註冊→auth、活動/抽獎→campaign、庫存→inventory（AC-GW-005）。

---

### ST-GW-006 — 公開功能免憑證

- **User Story:** As a 訪客 (PUBLIC), I want to 無需登入即可存取登入、註冊、活動查詢等公開功能, so that 我能先探索與建立身份再使用受保護功能。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-GW-06`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」（公開功能為身份驗證之豁免）
- **對應規格:** UC-4（gateway-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 無憑證請求命中公開功能，WHEN 辨識，THEN 跳過驗證、成功轉發（AC-GW-010）。
  - GIVEN 無憑證請求命中非公開功能，WHEN 驗證，THEN 回傳 `401`（公開清單封閉）（AC-GW-011）。
