# inventory-service — 使用情境 (User Stories)

> 對應 SA 規格：[inventory-service/README.md](../specs/inventory-service/README.md)。天條依據見 [requirements.md](../specs/requirements.md) §0。
>
> **定位**：inventory-service 無 end-user（USER/ADMIN）actor，是純後端協作服務，承擔「庫存真相來源」。故其 story 的「角色」為系統協作者，價值落在「不超抽」的一致性保證。

---

### ST-INV-001 — 執行庫存扣減（不為負）

- **User Story:** As a 平台（庫存真相來源）, I want to 在收到中獎扣減通知後執行庫存扣減且絕不使庫存為負, so that 實際發放獎品數絕不超過庫存。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-INV-01`, `FR-INV-02`
- **天條依據 (Source):** §0「防止獎品超過庫存被抽取」
- **對應規格:** UC-1（inventory-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 收到扣減通知且出貨真相剩餘 `> 0`，WHEN 處理，THEN 出貨真相扣減 `quantity`、標記「已完成」、確認通知（AC-INV-001）。
  - 語意：扣減以「剩餘 > 0」為最終保證，即使即時判定層誤判有貨也不扣成負數（UC-1 business rule）。

---

### ST-INV-002 — 扣減冪等（同一筆中獎只扣一次）

- **User Story:** As a 平台, I want to 使同一筆中獎的扣減通知重複投遞時只扣減一次, so that 重複通知不造成重複扣減庫存。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-INV-04`
- **天條依據 (Source):** §0「防止重複抽獎與獎品超抽」
- **對應規格:** UC-1（inventory-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 相同抽獎記錄的扣減通知重複投遞，WHEN 再次處理，THEN 不重複扣減、僅確認通知（AC-INV-003）。

---

### ST-INV-003 — 庫存不足補償（撤銷＋校正＋告警）

- **User Story:** As a 平台, I want to 在扣減發現庫存不足時撤銷該次中獎、校正即時判定層並發出告警, so that 系統不超發且異常可被察覺。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-INV-03`
- **天條依據 (Source):** §0「防止獎品超過庫存被抽取」
- **對應規格:** UC-2（inventory-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 出貨真相回報庫存不足，WHEN 偵測到，THEN 撤銷中獎結果、校正即時判定層（加回誤扣額度）、發出告警；庫存絕不為負（AC-INV-002）。

---

### ST-INV-004 — 定期帳目校對

- **User Story:** As a 平台, I want to 定期以出貨真相校正即時判定層並回收超時未完成的扣減, so that 庫存帳目收斂一致、額度不被永久佔用。
- **Priority:** Should
- **依賴需求 (Depends on):** `FR-INV-05`
- **天條依據 (Source):** §0「在高併發場景下，須確保事務一致性」（校對為一致性收斂手段；Should，非 POC 必須）
- **對應規格:** UC-3（inventory-service）
- **驗收意圖 (Acceptance intent):**
  - GIVEN 校對執行，WHEN 以真相校正，THEN 即時判定層收斂至出貨真相、無超發（AC-INV-004）。
  - GIVEN 存在超時未完成的扣減，WHEN 校對掃描，THEN 回收其額度（AC-INV-005）。
