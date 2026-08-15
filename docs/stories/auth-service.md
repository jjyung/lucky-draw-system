# auth-service — 使用情境 (User Stories)

> 對應 SA 規格：[auth-service/README.md](../specs/auth-service/README.md)。天條依據見 [requirements.md](../specs/requirements.md) §0。

---

### ST-AUTH-001 — 使用者註冊

- **User Story:** As a 訪客 (GUEST), I want to 以 username / email / password 註冊帳號, so that 我能取得參與抽獎所需的身份。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-AUTH-01`, `FR-AUTH-06`, `FR-X-01`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」「完整的錯誤流程處理與輸入驗證」
- **對應規格:** UC-1（auth-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 提交合法且唯一的 username/email/password，WHEN 註冊，THEN 建立帳號（預設 `ROLE_USER`）、密碼以不可逆雜湊儲存、回應不含密碼或雜湊（AC-AUTH-001）。
  - GIVEN username 或 email 已被使用，WHEN 註冊，THEN 回傳 `409`，不建立第二帳號（AC-AUTH-002）。
  - GIVEN 缺欄位／email 格式非法／密碼為空，WHEN 註冊，THEN 回傳驗證錯誤且不建立帳號（AC-AUTH-003）。

---

### ST-AUTH-002 — 使用者登入取得身份憑證

- **User Story:** As a 已註冊使用者, I want to 以帳號密碼登入並取得承載身份與角色的身份憑證, so that 我能通過 Gateway 驗證並存取抽獎等受保護功能。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-AUTH-02`, `FR-AUTH-03`, `FR-X-01`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」
- **對應規格:** UC-2（auth-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 正確憑證，WHEN 登入，THEN 簽發身份憑證（承載身份/角色/有效期/簽發時刻/簽發者），身份與角色正確（AC-AUTH-004）。
  - GIVEN 密碼錯誤，WHEN 登入，THEN 回傳 `401`、不簽發（AC-AUTH-005）。
  - GIVEN 帳號不存在，WHEN 登入，THEN 回傳 `401` 且與密碼錯誤**不可區分**（AC-AUTH-006）。
  - GIVEN 已簽發憑證，WHEN 以公開資訊驗證，THEN 驗證通過且簽發機密無法經任何端點取得、非簽發方無法偽造（AC-AUTH-007/008）。
  - GIVEN 憑證已過期，WHEN 用於受保護請求，THEN 回傳 `401`（AC-AUTH-012）。

---

### ST-AUTH-003 — 登入有效期延續

- **User Story:** As a 已登入使用者, I want to 以有效刷新憑證換發新存取憑證, so that 我無需重新登入即可延長登入有效期。
- **Priority:** Should
- **依賴需求 (Depends on):** `FR-AUTH-04`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」（refresh 為細化；Should，非 POC 必須）
- **對應規格:** UC-3（auth-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 有效刷新憑證（未過期），WHEN 提交刷新，THEN 取得新存取憑證、身份/角色不變（AC-AUTH-013）。
  - GIVEN 過期或無效刷新憑證，WHEN 提交刷新，THEN 回傳 `401`、須重新登入（AC-AUTH-014）。

---

### ST-AUTH-004 — 權限分級

- **User Story:** As a 系統／平台, I want to 區分 `ROLE_USER` 與 `ROLE_ADMIN` 兩級權限並由憑證承載, so that 管理功能僅限管理員、使用者無法自我提權。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-AUTH-05`, `FR-X-01`
- **天條依據 (Source):** §0「API 支援身份驗證與權限分級」
- **對應規格:** UC-4（auth-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 憑證角色為 `ROLE_USER`，WHEN 存取管理功能，THEN 回傳 `403`（AC-AUTH-009）。
  - GIVEN 憑證角色為 `ROLE_ADMIN`，WHEN 存取管理功能，THEN 授權放行（AC-AUTH-010）。
  - GIVEN client 嘗試在請求中指定/修改角色，WHEN 系統處理，THEN 以憑證承載角色為準、client 指定被忽略（AC-AUTH-011）。
