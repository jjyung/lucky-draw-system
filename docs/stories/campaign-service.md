# campaign-service — 使用情境 (User Stories)

> 對應 SA 規格：[campaign-service/README.md](../specs/campaign-service/README.md)。天條依據見 [requirements.md](../specs/requirements.md) §0。

---

### ST-CAMP-001 — 建立／編輯抽獎活動

- **User Story:** As a 營運人員 (ADMIN), I want to 建立並編輯抽獎活動（名稱、起訖時間、個人抽獎次數上限）並管理其狀態, so that 我能控制抽獎的開放時間與每人可抽次數。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-01`, `FR-CAMP-11`, `FR-X-01`
- **天條依據 (Source):** §0「不同抽獎活動可設定各自的抽獎次數上限」
- **對應規格:** UC-1, UC-3（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 合法活動資訊，WHEN ADMIN 建立，THEN 建立成功且狀態為 `DRAFT`（UC-1 acceptance intent）。
  - GIVEN 非 `ACTIVE` 活動，WHEN USER 提交抽獎，THEN 回傳 `404`/`409`、不執行抽獎（AC-CAMP-005）。
  - GIVEN 非法狀態轉移（如 `ENDED` 後再啟用），WHEN ADMIN 操作，THEN 回傳 `409` 衝突（UC-3 acceptance intent）。

---

### ST-CAMP-002 — 配置獎品與機率（含銘謝惠顧）

- **User Story:** As a 營運人員 (ADMIN), I want to 為活動配置多個獎品（名稱、庫存、機率）並包含「銘謝惠顧」，且機率總和強制為 100%, so that 抽獎的機率分布正確且可動態調整。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-02`, `FR-CAMP-03`, `FR-CAMP-04`, `FR-CAMP-06`
- **天條依據 (Source):** §0「多種獎品且可設定獎品數量（庫存）與對應中獎機率」「銘謝惠顧作為無獎品選項，與各獎品機率總和為 100%」
- **對應規格:** UC-2（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 機率總和 ≠ 100%（超出浮點容差），WHEN 配置，THEN 回傳 `400`/`422` 且整筆不生效（AC-CAMP-001）。
  - GIVEN 任一獎品機率越界 `[0,100]`，WHEN 配置，THEN 回傳驗證錯誤、不生效（AC-CAMP-002）。
  - GIVEN 配置缺 `type=THANK_YOU`，WHEN 配置，THEN 回傳驗證錯誤、不生效（AC-CAMP-003）。

---

### ST-CAMP-003 — 動態修改獎品內容

- **User Story:** As a 營運人員 (ADMIN), I want to 動態修改獎品名稱、數量與機率, so that 變更於後續抽獎生效而無需改 code 或重啟。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-05`
- **天條依據 (Source):** §0「獎品內容（名稱、數量、機率等）需可透過動態配置修改」
- **對應規格:** UC-2（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 修改後的合法配置，WHEN 生效，THEN 後續抽獎採用新機率、已發生結果不受影響（UC-2 acceptance intent）。

---

### ST-CAMP-004 — 單次抽獎

- **User Story:** As a 抽獎者 (USER), I want to 對 `ACTIVE` 活動提交單次抽獎, so that 我能依機率獲得一個中獎結果或銘謝惠顧。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-07`, `FR-CAMP-10`, `FR-CAMP-17`, `FR-CAMP-18`, `FR-CAMP-19`, `FR-X-01`
- **天條依據 (Source):** §0「支援單次抽獎與多次連續抽獎」「防止重複抽獎與獎品超抽」
- **對應規格:** UC-4（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 活動 `ACTIVE`、USER 有剩餘次數、有效冪等識別，WHEN 提交單次抽獎，THEN 依機率選取一獎品、記錄一筆結果、計次 1 次、回傳一個結果（AC-CAMP-004）。
  - GIVEN 命中獎品但庫存不足，WHEN 抽獎，THEN 降級銘謝惠顧、不重抽、不通知扣減、仍計次（AC-CAMP-014）。
  - GIVEN 結果為中獎，WHEN 抽獎完成，THEN 通知庫存服務扣減且對該記錄僅通知一次（AC-CAMP-016）。

---

### ST-CAMP-005 — 批次抽獎

- **User Story:** As a 抽獎者 (USER), I want to 以單一請求抽 N 次（count=N）, so that 我能一次性取得多筆結果。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-08`, `FR-CAMP-10`, `FR-CAMP-15`, `FR-CAMP-17`, `FR-CAMP-18`, `FR-CAMP-19`, `FR-X-02`
- **天條依據 (Source):** §0「支援單次抽獎與多次連續抽獎」
- **對應規格:** UC-5（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN `count=N` 且剩餘次數 ≥ N，WHEN 提交，THEN 回傳 N 筆結果、每筆命中獎品時逐筆確認庫存（AC-CAMP-008）。
  - GIVEN 批次成功，WHEN 完成，THEN 個人次數一次扣 N、僅一次（AC-CAMP-009）。
  - GIVEN 批次中每筆中獎，WHEN 抽獎完成，THEN 對每筆中獎通知庫存扣減（FR-CAMP-17）。
  - GIVEN 剩餘次數 < N，WHEN 提交，THEN 回傳 `429`、整批不執行、不產生部分結果（AC-CAMP-010）。

---

### ST-CAMP-006 — 並發多個單次抽獎

- **User Story:** As a 抽獎者 (USER)／前端, I want to 同時發出多個單次抽獎請求（各帶獨立冪等識別）, so that 我能以並發方式達成多次抽獎且各自獨立計次。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-09`
- **天條依據 (Source):** §0「可有同時多次抽獎的機會」
- **對應規格:** UC-4（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 前端同時發出 N 個單次請求、各帶獨立冪等識別，WHEN 系統並行處理，THEN 各請求各自計次、各自扣庫存，互不誤判為重複（AC-CAMP-011）。

---

### ST-CAMP-007 — 個人抽獎次數上限

- **User Story:** As a 抽獎者 (USER)／平台, I want to 受「活動期間總額」抽獎次數上限約束, so that 同一使用者無法超出其允許的抽獎次數。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-11`, `FR-CAMP-12`
- **天條依據 (Source):** §0「同一使用者不可超出其允許的抽獎次數」「不同抽獎活動可設定各自的抽獎次數上限」
- **對應規格:** UC-1, UC-4, UC-5（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN USER 累計已達 `draw_limit`，WHEN 提交抽獎，THEN 回傳 `429`、不記錄/不扣庫存/不計次（AC-CAMP-006）。
  - GIVEN `draw_limit=10` 跨多日，WHEN USER 累計抽滿，THEN 跨日累計、非每日重置（AC-CAMP-007）。

---

### ST-CAMP-008 — 防重複抽獎與 replay

- **User Story:** As a 抽獎者 (USER)／平台, I want to 使同一抽獎請求重送時只產生一次結果並回傳相同結果, so that 網路重試/重送不會重複中獎或重複扣次。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-13`, `FR-CAMP-14`
- **天條依據 (Source):** §0「防止重複抽獎」
- **對應規格:** UC-4, UC-5（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 一筆抽獎已完成，WHEN 相同請求重送，THEN 回傳與第一次完全相同結果、不重抽/不重扣庫存/不重扣次數（AC-CAMP-012）。
  - GIVEN 批次已完成，WHEN 相同請求重送，THEN 回傳原 N 筆結果、不重複副作用（AC-CAMP-013）。

---

### ST-CAMP-010 — 瀏覽活動（列表＋詳情）

- **User Story:** As a 訪客／抽獎者 (PUBLIC/USER), I want to 瀏覽活動列表並查看活動詳情, so that 我能決定是否參與抽獎。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-01`（活動 CRUD 之 R）、`FR-GW-06`（公開功能）
- **天條依據 (Source):** §0「不同抽獎活動可設定各自的抽獎次數上限」（活動須可被查詢才能參與；查詢為公開功能，見 FR-GW-06）
- **對應規格:** UC-6（campaign-service，本缺口回寫新增）
- **對應 API:** `campaign-campaigns-001`（列表）、`campaign-campaigns-002`（詳情）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 未登入請求，WHEN 存取活動列表/詳情，THEN 免憑證放行（對應 AC-GW-010）。
  - GIVEN 活動列表，WHEN 查詢，THEN 回傳活動資訊、不含管理欄位。
  - GIVEN 活動詳情，WHEN 查詢，THEN 回傳該活動資訊（名稱、時間、狀態、獎品概要），供抽獎前展示。

---

### ST-CAMP-009 — 防超抽（庫存確認＋降級銘謝惠顧）

- **User Story:** As a 平台, I want to 在抽中獎品前確認庫存、不足時降級銘謝惠顧, so that 獎品實際發放絕不超過庫存。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-CAMP-18`, `FR-CAMP-19`
- **天條依據 (Source):** §0「防止獎品超過庫存被抽取」
- **對應規格:** UC-4, UC-5（campaign-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 抽選命中獎品但庫存不足，WHEN 抽獎執行，THEN 結果降級 `THANK_YOU`、不重抽、不通知扣減、本次仍計次（AC-CAMP-014）。
  - 語意：庫存「足夠才確認中獎，不足視為未中獎」，不重抽以免改寫機率分布（UC-4 business rule）。
