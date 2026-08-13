# campaign-service — SA 業務需求 (Business Requirements)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SA — Business Requirements（業務行為與語意） |
| **服務範圍** | campaign-service（活動與獎品管理、抽獎邏輯、冪等控制、通知庫存扣減） |

### 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [requirements.md](../requirements.md) | 主需求清單，本文件逐一對齊其 `FR-CAMP-*`、`FR-X-*` 與相關 `NFR` |
| [AGENTS.md](../../../AGENTS.md) | 開發流程指引，本文件遵循其 §3 SA 層模板 |
| [ADR-004](../../adr/004-weighted-draw-algorithm.md) | 抽獎選取（銘謝惠顧建模、機率總和 100%） |
| [ADR-005](../../adr/005-anti-double-draw-idempotency.md) | 防重複抽獎與冪等（replay 語意） |
| [ADR-006](../../adr/006-anti-overselling.md) | 防超抽（庫存確認 + 庫存不足降級） |
| [ADR-007](../../adr/007-async-kafka-spring-cloud-stream.md) | 通知庫存扣減的協作機制 |
| [draw-flow.md](../../architecture/draw-flow.md) | 抽獎生命週期與失敗路徑（本文件行為描述的執行細節來源） |
| [risk-control.md](../../architecture/risk-control.md) | 併發控制與最終一致性 |

> **層級界線**：本文件只定義**業務行為與語意**（use case、business rule、business state、acceptance intent、business data dictionary）。API 路由/參數、API 文件、資料庫 schema、併發控制實作細節屬 **SD 層**，不在本文件範圍；本文件引用之 ADR 中的技術細節僅作為業務語意之佐證，不在此重複設計。

---

## 2. 系統／服務定位

### 2.1 Problem & Goal

抽獎是「高價值、非冪等、短暫高併發」的商業動作。營運需要**在不改 code 的前提下**動態配置「多種獎品（各有庫存與中獎機率）＋銘謝惠顧（總和 100%）」，讓使用者進行**單次或多次抽獎**，同時在風控機制下**防止重複抽獎**（同一請求不可重複出獎/扣次）與**獎品超抽**（實發數絕不超過庫存）。

campaign-service 是此能力的承載者：它回答「**系統要提供什麼抽獎行為**」，並以業務語意約束「結果如何才算符合目的」。庫存的真相來源（實際出貨扣減）由 inventory-service 承擔，campaign-service 只透過「庫存確認」與「通知扣減」與其協作。

### 2.2 Actors & User Roles

| Role | 說明 | 主要能力 |
|------|------|----------|
| **ADMIN**（`ROLE_ADMIN`） | 營運人員 | 建立/編輯活動、配置獎品與機率、管理活動狀態 |
| **USER**（`ROLE_USER`） | 一般使用者（抽獎者） | 單次抽獎、批次抽獎 |

> 角色與權限判定由 Auth Service + API Gateway 提供（ADR-009），campaign-service **信任 Gateway 轉發的身分 claim**（`X-User-Id`、`X-User-Roles`），並在業務層再以角色約束操作範圍。

### 2.3 Business Capabilities（本服務提供的能力）

1. 抽獎活動生命週期管理（建立、編輯、狀態機）
2. 獎品與機率配置（含銘謝惠顧、總和 100% 驗證）
3. 權重隨機抽獎（單次／批次／並發單次）
4. 個人抽獎次數上限控管（**活動期間總額**）
5. 防重複抽獎（複合冪等鍵 + replay 語意）
6. 通知庫存扣減（抽中獎品時）

---

## 3. Use Cases

> 每個 use case 依 AGENTS.md §3 格式：Use case name / Actor / Precondition / Main flow（編號步驟）/ Business rule / Acceptance intent，並帶 **Traceability** 行標註其實現的 `FR-*`。

---

### UC-1 建立／編輯抽獎活動 (Create/Edit Campaign)

- **Actor:** ADMIN
- **Precondition:** 呼叫者具 `ROLE_ADMIN`；活動必填欄位合法。
- **Main flow:**
  1. ADMIN 提交活動資訊（名稱、開始時間、結束時間、活動期間總額抽獎次數上限）。
  2. 系統驗證輸入（名稱非空、時間先後合法、次數上限為正整數）。
  3. 系統建立活動，初始狀態為 `DRAFT`；編輯時僅允許在可編輯狀態下修改。
  4. 系統回傳成功結果與活動識別。
- **Business rule:**
  - 活動建立後初始狀態為 `DRAFT`，尚不可被 USER 抽獎。
  - `draw_limit`（個人抽獎次數上限）為**活動期間總額**：每個使用者於本活動整個週期最多 N 次，**非每日重置**。
  - `end_time` 是抽獎的業務終點，也是個人次數計數的到期邊界（TTL 對齊活動結束）。
  - 編輯不得造成既有抽獎記錄失效或已抽結果回溯變更（編輯只影響後續抽獎）。
- **Acceptance intent:**
  - 合法輸入可成功建立活動，狀態為 `DRAFT`。
  - 非 ADMIN 呼叫被拒；非法輸入（負數上限、結束早於開始）回傳可理解的驗證錯誤。
  - 活動期間總額次數上限語意正確（跨日累計，非每日重置）。
- **Traceability:** `FR-CAMP-01`, `FR-CAMP-11`, `FR-X-01`

---

### UC-2 配置獎品與機率 (Configure Prizes & Probabilities)

- **Actor:** ADMIN
- **Precondition:** 活動已存在且可編輯（`DRAFT` 或後續動態修改）；呼叫者具 `ROLE_ADMIN`。
- **Main flow:**
  1. ADMIN 提交獎品清單：每個獎品含名稱、型別（`PRIZE` 或 `THANK_YOU`）、庫存數量、中獎機率；並包含至少一個「銘謝惠顧」。
  2. 系統驗證每個獎品機率介於 `[0, 100]`。
  3. 系統驗證**所有獎品（含銘謝惠顧）機率總和 = 100%**（浮點容差內）。
  4. 系統驗證至少存在一個 `type = THANK_YOU` 獎品。
  5. 驗證失敗 → 系統回傳錯誤（`400`/`422`），**整筆配置不生效**；驗證成功 → 生效，於後續抽獎採用。
- **Business rule:**
  - 「銘謝惠顧」建模為 `type = THANK_YOU` 的獎品，與一般獎品（`type = PRIZE`）同列於獎品清單、同具機率欄位。
  - 機率語意：**所有獎品機率總和必須等於 100%**，否則系統在配置時間拒絕，不進入 runtime。
  - 每個獎品機率必須落在 `[0, 100]`；`THANK_YOU` 機率亦在範圍內（`[0,100]`，通常為剩餘未中獎機率）。
  - 獎品內容（名稱、數量、機率）可**動態修改**，修改後於**後續抽獎**生效；已發生的抽獎結果不受影響。
  - 庫存數量為 ADMIN 初始配置；實際剩餘庫存之真相來源在 inventory-service，campaign-service 的數量欄位為配置語意。
- **Acceptance intent:**
  - 總和 ≠ 100% 或機率越界或缺銘謝惠顧 → 拒絕且不生效，錯誤可理解。
  - 合法配置（總和 = 100%、含銘謝惠顧）成功生效。
  - 動態改機率後，新抽獎使用新機率，歷史結果不變。
- **Traceability:** `FR-CAMP-02`, `FR-CAMP-03`, `FR-CAMP-04`, `FR-CAMP-05`, `FR-CAMP-06`

---

### UC-3 活動狀態管理 (Campaign State Management)

- **Actor:** ADMIN
- **Precondition:** 活動已存在；呼叫者具 `ROLE_ADMIN`。
- **Main flow:**
  1. ADMIN 觸發狀態轉移（如啟用、結束）。
  2. 系統依狀態機驗證轉移合法性。
  3. 合法 → 系統更新狀態並回傳；非法 → 回傳衝突錯誤（`409`）。
- **Business rule:**
  - 狀態機：`DRAFT → ACTIVE → ENDED`（詳見 §4）。
  - 僅 ADMIN 可變更活動狀態；USER 無法改任何狀態。
  - 只有 `ACTIVE` 狀態的活動可供 USER 抽獎。
  - 結束（`ENDED`）為終態，不可回轉。
- **Acceptance intent:**
  - 合法轉移成功；非法轉移（如 `ENDED` 後再啟用）回傳可理解衝突。
  - 非 `ACTIVE` 活動之抽獎請求被拒（`404`/`409`）。
- **Traceability:** `FR-CAMP-01`, `FR-X-01`

---

### UC-4 單次抽獎 (Single Draw)

- **Actor:** USER
- **Precondition:** 活動狀態為 `ACTIVE`；呼叫者具 `ROLE_USER`；請求帶有效 `Idempotency-Key`；個人於本活動的累計抽獎次數未達 `draw_limit`。
- **Main flow:**
  1. USER 提交單次抽獎請求（`count = 1`，附獨立 `Idempotency-Key`）。
  2. 系統以 `userId + campaignId + idempotencyKey` 為複合冪等鍵，先查是否已有結果（replay 判定）。
  3. 若有既有結果 → 回傳原始結果（不重抽、不重扣）；若無 → 執行抽獎。
  4. 系統檢查個人剩餘抽獎次數（活動期間總額）；超限 → 回傳 `429`，不抽。
  5. 系統以權重隨機演算法選取一個獎品（單一 random double in `[0,100)` 走累計機率區間）。
  6. 若命中獎品 → 確認庫存；庫存不足 → **降級為銘謝惠顧（不重抽）**。
  7. 系統記錄一筆抽獎結果（含結果快照與冪等識別）；計入抽獎次數 1 次。
  8. 若中獎品 → 通知庫存服務扣減；回傳抽獎結果給 USER。
- **Business rule:**
  - 抽獎結果由**伺服端演算法決定**，client 不得指定或影響中獎結果。
  - 個人次數上限為**活動期間總額**（維度 = campaign），超限回傳 `429 Too Many Requests`。
  - 複合冪等鍵 `userId + campaignId + idempotencyKey`：相同鍵的請求（含並發重入、超時重試）只產生一筆結果。
  - **replay 語意**：相同複合鍵重複請求回傳與第一次完全相同的結果，**不重抽、不重扣庫存、不重扣次數**。
  - 庫存不足時**降級為銘謝惠顧，不重抽**（避免重抽改寫機率分布）；抽獎次數仍計 1 次（本次請求確實執行了抽獎）。
  - **並發多個單次請求**（前端發出 N 個、各帶獨立 Idempotency-Key）語意等同 N 次獨立單次抽獎，各自計次、各自扣庫存。
- **Acceptance intent:**
  - 合法單次抽獎回傳一個中獎結果或銘謝惠顧，且記錄一筆結果、計次 1 次。
  - 超限（活動總額）回傳 `429`，不產生新記錄、不扣庫存。
  - 同鍵重送回傳原結果，不產生第二筆記錄、不重扣。
  - 庫存不足時回傳銘謝惠顧，不重抽、不通知庫存扣減。
- **Traceability:** `FR-CAMP-07`, `FR-CAMP-09`, `FR-CAMP-10`, `FR-CAMP-11`, `FR-CAMP-12`, `FR-CAMP-13`, `FR-CAMP-14`, `FR-CAMP-17`, `FR-CAMP-18`, `FR-CAMP-19`, `FR-X-01`

---

### UC-5 批次抽獎 count = N (Batch Draw)

- **Actor:** USER
- **Precondition:** 活動狀態為 `ACTIVE`；呼叫者具 `ROLE_USER`；請求帶單一有效 `Idempotency-Key`；`count = N ≥ 2` 且不超過個人剩餘抽獎次數。
- **Main flow:**
  1. USER 提交批次抽獎請求（`count = N`，**單一 Idempotency-Key 對應整批**）。
  2. 系統以複合冪等鍵查 replay；無既有結果則進入批次執行。
  3. 系統檢查個人剩餘抽獎次數是否 ≥ N；不足則按超限規則處理（回傳 `429`，整批不執行）。
  4. 系統對整批執行 N 次獨立抽選；每次抽選命中獎品時**逐筆各自**確認庫存，庫存不足者**逐筆降級銘謝惠顧**。
  5. 系統記錄 **N 筆抽獎結果**；整批**一次性扣除 N 次**抽獎次數。
  6. 對每筆中獎品的結果通知庫存扣減；回傳 N 筆結果。
- **Business rule:**
  - 批次抽獎 = 「單一請求、伺服端 N 次獨立抽選」，整批由**單一冪等識別**保護。
  - 整批副作用（N 筆結果記錄、N 次計次、N 筆庫存扣減通知）**僅執行一次**；replay 不重複執行。
  - 抽獎次數**一次扣 N**（僅成功產生結果的請求計次）。
  - 每筆抽選獨立計算機率與庫存，互不影響（一筆庫存不足不影響同批其他筆）。
  - 批次與「並發多個單次請求」（UC-4）為**兩種並存模式**，都需支援。
- **Acceptance intent:**
  - `count = N` 請求回傳 N 筆結果、記錄 N 筆結果、計次 N 次。
  - 剩餘次數不足 N 時整批不執行，回傳 `429`，不產生部分結果。
  - 同鍵 replay 回傳與第一次完全相同的 N 筆結果，不重落、不重扣、不重計。
  - 整批中部分獎品庫存不足者，該筆降級銘謝惠顧，其餘筆不受影響。
- **Traceability:** `FR-CAMP-08`, `FR-CAMP-10`, `FR-CAMP-15`, `FR-CAMP-18`, `FR-CAMP-19`, `FR-X-02`

---

## 4. Business State

### 4.1 活動生命週期 (Campaign Lifecycle)

```
DRAFT ──► ACTIVE ──► ENDED (終態)
```

| 狀態 | 業務意義 | 可抽獎？ | 可編輯配置？ |
|------|----------|----------|--------------|
| `DRAFT` | 草稿，營運尚未開放 | 否 | 是（全量） |
| `ACTIVE` | 活動進行中（在 start~end 時間窗內） | 是 | 動態修改獎品內容（後續抽獎生效） |
| `ENDED` | 已結束（到 end_time 或手動結束） | 否 | 否 |

**狀態轉移規則（state machine semantics）：**

| 轉移 | 允許 | 說明 |
|------|------|------|
| `DRAFT → ACTIVE` | ✅ | 單向；啟用後不可退回 `DRAFT` |
| `ACTIVE → ENDED` | ✅ | 手動結束或到達 `end_time` |
| `ENDED → *` | ❌ | 終態，不可回轉 |

### 4.2 抽獎結果型別 (draw_record result types)

| 結果型別 | 業務意義 | 來源 |
|----------|----------|------|
| `THANK_YOU` | 銘謝惠顧，未中獎品 | 權重抽選命中 `THANK_YOU` 獎品；或命中獎品但庫存不足而**降級** |
| `WIN` | 中獎（命中任一獎品） | 抽選命中獎品且庫存確認足夠 |

> **註**：`VOID`（撤銷）是 inventory-service 在庫存扣減發現不足時的**補償狀態**（ADR-006 Path C），屬下游補償語意，非抽獎本身之正常結果；campaign-service 的業務結果型別僅上述兩種，`VOID` 由下游回寫，SA 於此僅註記其存在。

### 4.3 業務語意問題（AGENTS.md §3 要求回答）

1. **狀態能否回轉？**
   - `DRAFT → ACTIVE` 單向（啟用不可退回草稿）。
   - `ENDED` 為終態，**不可回轉**；到達 `end_time` 視同結束。

2. **使用者看到的是本系統狀態還是 upstream 狀態？**
   - 活動狀態（`DRAFT/ACTIVE/ENDED`）是 campaign-service **本系統狀態**。
   - 獎品**實際剩餘庫存**是 inventory-service 的 upstream 狀態；campaign-service 只透過「庫存確認」與「通知扣減」協作，不持有庫存真相。USER 看到的「可抽性」由本系統活動狀態決定，「庫存精確餘額」不即時暴露。

3. **哪些角色能改狀態？**
   - 活動狀態：僅 `ADMIN`。
   - 抽獎結果 / 記錄：無人可改（系統產生，USER 只讀、ADMIN 亦不可改寫已產生之結果）。

4. **哪些欄位不能被使用者直接改？**
   - 中獎結果（`result_type` / `prize_id`）：由伺服端抽獎邏輯決定，client **不得指定**。
   - 個人抽獎次數：伺服端計數，client 不得增減。
   - 冪等識別中的使用者/活動：由伺服端依憑證與請求路徑決定，client 僅提供冪等識別。
   - 獎品機率 / 庫存 / 名稱：僅 `ADMIN` 可改，USER 不得修改。

---

## 5. Business Data Dictionary

> 本表定義**欄位的業務意義**（SA 層）。型別、資料庫 schema、constraint、index 屬 SD 之 Technical Data Dictionary，**不在本表定義**。

### 5.1 campaign（活動）

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `id` | 活動唯一識別 | 是（系統產生） | 系統產生之唯一值 | campaign-service | internal |
| `name` | 活動名稱，營運與使用者可辨識 | 是 | 非空字串 | ADMIN 輸入 | internal |
| `status` | 活動生命週期狀態 | 是 | `DRAFT` / `ACTIVE` / `ENDED` | campaign-service 狀態機 | internal |
| `start_time` | 活動開始時間（可抽獎起點） | 是 | 合法時間，早於 `end_time` | ADMIN 輸入 | internal |
| `end_time` | 活動結束時間（可抽獎終點；個人次數計數 TTL 對齊此時間） | 是 | 合法時間，晚於 `start_time` | ADMIN 輸入 | internal |
| `draw_limit` | **每個使用者於本活動整個週期的總抽獎次數上限**（活動期間總額，非每日） | 是 | 正整數 ≥ 1 | ADMIN 輸入 | internal |

### 5.2 prize（獎品）

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `name` | 獎品名稱 | 是 | 非空字串 | ADMIN 輸入 | internal |
| `type` | 獎品型別 | 是 | `PRIZE` / `THANK_YOU` | ADMIN 輸入 | internal |
| `probability` | 中獎機率（百分比）；含銘謝惠顧在內，全體總和 = 100% | 是 | `[0, 100]`；全體總和 = 100%（浮點容差內） | ADMIN 輸入 | sensitive（營運參數） |
| `stock` / `quantity` | 可發放數量（銘謝惠顧不適用，視為無限） | 是（`PRIZE`） | 非負整數；`THANK_YOU` 不適用 | ADMIN 初始配置；實際庫存真相在 inventory-service | sensitive（營運） |

### 5.3 抽獎記錄 (draw_record)

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `user_id` | 執行抽獎之使用者 | 是 | 有效使用者識別 | 由憑證決定（Gateway 傳遞），非 client 指定 | personal |
| `campaign_id` | 抽獎所屬活動 | 是 | 有效活動識別 | 由請求路徑決定（伺服端不信任 client） | internal |
| `idempotency_key` | 冪等識別中 client 提供之部分（一次點擊一個識別） | 是 | 非空識別 | Client 提供（Gateway 強制存在） | internal |
| `result_type` | 本次抽獎結果型別 | 是 | `WIN` / `THANK_YOU` | campaign-service 抽獎邏輯（伺服端決定） | internal |
| `prize_id` | 中獎獎品識別（銘謝惠顧時指向 `THANK_YOU` 獎品） | 條件 | 有效獎品識別 | 抽獎邏輯結果 | internal |
| 抽獎次數 (draw count) | 使用者於活動期間之**累計**抽獎次數（派生計數，維度 = 活動） | 派生 | 非負整數 ≤ `draw_limit` | campaign-service 計數；抽獎記錄為稽核真相 | personal |

> **語意註記**：抽獎次數是「使用者 × 活動」維度的派生計數，不是單一抽獎記錄的欄位；抽獎記錄是該計數的稽核證據（每筆成功抽獎對應 +1，批次 +N）。

---

## 6. Acceptance Criteria

> 每條以 GIVEN/WHEN/THEN 描述，標註 AC ID 與對應 FR。

### 6.1 配置驗證 (Config Validation)

**AC-CAMP-001 — 機率總和必須等於 100%**（`FR-CAMP-04`）
- GIVEN ADMIN 提交一份獎品配置（含銘謝惠顧）
- WHEN 所有獎品機率總和 ≠ 100%（超出浮點容差）
- THEN 系統回傳驗證錯誤（`400`/`422`），且整筆配置**不生效**，活動仍為原有效配置

**AC-CAMP-002 — 機率範圍 [0, 100]**（`FR-CAMP-06`）
- GIVEN 一份獎品配置
- WHEN 任一獎品 `probability < 0` 或 `> 100`
- THEN 系統回傳驗證錯誤，配置不生效

**AC-CAMP-003 — 至少一個銘謝惠顧**（`FR-CAMP-06`）
- GIVEN 一份獎品配置
- WHEN 配置中不存在 `type = THANK_YOU` 的獎品
- THEN 系統回傳驗證錯誤（因活動將無法產生「未中獎」結果），配置不生效

### 6.2 單次抽獎 (Single Draw)

**AC-CAMP-004 — 單次抽獎成功**（`FR-CAMP-07`, `FR-CAMP-10`）
- GIVEN 活動為 `ACTIVE`、USER 有剩餘次數、請求帶有效冪等識別
- WHEN USER 提交單次抽獎（`count = 1`）
- THEN 系統依機率選取一個獎品、記錄一筆結果、計次 1 次、回傳一個結果（中獎或銘謝惠顧）

**AC-CAMP-005 — 活動狀態門檻**（`FR-CAMP-01`）
- GIVEN 活動狀態為 `DRAFT` / `ENDED`
- WHEN USER 提交抽獎請求
- THEN 系統回傳 `404`（不存在/未啟動）或 `409`（狀態衝突），不執行抽獎、不扣任何資源

### 6.3 抽獎次數上限 (Draw Count Limit — 活動期間總額)

**AC-CAMP-006 — 上限強制（活動總額）**（`FR-CAMP-11`, `FR-CAMP-12`）
- GIVEN USER 於本活動的累計抽獎次數已達 `draw_limit`
- WHEN USER 提交抽獎請求
- THEN 系統回傳 `429 Too Many Requests`，不記錄結果、不扣庫存、不計次

**AC-CAMP-007 — 非每日重置**（`FR-CAMP-11`）
- GIVEN 活動 `draw_limit = 10` 且跨越多日
- WHEN USER 於不同日期累計抽滿 10 次
- THEN 系統仍視為已達上限（計數跨日累計），而非每日重新給 10 次

### 6.4 批次抽獎 (Batch Draw)

**AC-CAMP-008 — 批次抽獎語意**（`FR-CAMP-08`）
- GIVEN 活動 `ACTIVE`、USER 剩餘次數 ≥ N、單一冪等識別
- WHEN USER 提交 `count = N` 批次抽獎
- THEN 系統記錄 N 筆結果、回傳 N 筆結果、每筆命中獎品時**逐筆各自**確認庫存

**AC-CAMP-009 — 批次一次扣 N 次**（`FR-CAMP-15`）
- GIVEN `count = N` 批次抽獎成功
- WHEN 批次完成
- THEN 個人抽獎次數**一次扣除 N**，且只發生一次

**AC-CAMP-010 — 批次不足整批不執行**（`FR-CAMP-15`, `FR-X-01`）
- GIVEN USER 剩餘次數 < N
- WHEN 提交 `count = N`
- THEN 回傳 `429`，整批不執行、不產生部分結果

### 6.5 並發與冪等 (Concurrency & Idempotency)

**AC-CAMP-011 — 並發多個單次請求各自獨立**（`FR-CAMP-09`）
- GIVEN 前端同時發出 N 個單次請求、各帶獨立冪等識別
- WHEN 系統並行處理
- THEN 各請求各自計次、各自扣庫存，語意等同 N 次獨立單次抽獎，互不誤判為重複

**AC-CAMP-012 — Replay 回傳原始結果**（`FR-CAMP-13`, `FR-CAMP-14`）
- GIVEN 一筆抽獎已成功完成
- WHEN 相同請求重送
- THEN 系統回傳與第一次**完全相同**的結果（同 response），**不重抽、不重扣庫存、不重扣次數**

**AC-CAMP-013 — 批次 replay 不重複副作用**（`FR-CAMP-14`, `FR-X-02`）
- GIVEN 一筆 `count = N` 批次抽獎已成功完成
- WHEN 相同請求重送
- THEN 回傳原 N 筆結果，不重複記錄、不重複扣 N 次、不重複通知扣減

### 6.6 防超抽降級 (Anti-Overselling Degrade)

**AC-CAMP-014 — 庫存不足降級銘謝惠顧（不重抽）**（`FR-CAMP-19`, `FR-CAMP-10`）
- GIVEN 抽選命中獎品但該獎品庫存不足
- WHEN 抽獎流程執行
- THEN 結果降級為 `THANK_YOU`、記錄一筆銘謝惠顧、**不重抽**、不通知庫存扣減、本次仍計次 1 次

### 6.7 機率分布正確性 (Probability Distribution)

**AC-CAMP-015 — 機率分布收斂**（`FR-CAMP-10`, `NFR-07`）
- GIVEN 一份合法配置（如 p1=5%, p2=15%, p3=30%, THANK_YOU=50%）
- WHEN 執行大量抽樣（統計意義上足夠多次）
- THEN 各獎品命中比例收斂至配置機率（誤差在統計容許範圍內）

### 6.8 通知庫存扣減 (Inventory Deduction Notification)

**AC-CAMP-016 — 中獎通知庫存扣減**（`FR-CAMP-17`）
- GIVEN 抽獎結果為中獎（`WIN`）
- WHEN 抽獎完成
- THEN 系統通知庫存服務扣減（含抽獎記錄識別、獎品識別、數量），且對該記錄僅通知一次

---

## 7. Out of Scope（本 SA 文件不涵蓋）

| 項目 | 說明 | 後續層 |
|------|------|--------|
| 身份驗證 / 授權實作 | 身份驗證、權限判定屬 Auth Service + API Gateway（`FR-GW-*`、`FR-AUTH-*`），campaign-service 僅信任傳遞的身份與角色 | Auth/Gateway spec |
| 庫存真相來源與補償內部邏輯 | 庫存扣減、撤銷、帳目校對屬 inventory-service（`FR-INV-*`） | inventory-service spec |
| API 路由 / 參數 / 文件 | 具體端點、request/response schema、status code 對映表屬 SD（`FR-X-03`, `FR-X-04`） | SD / `docs/api/` |
| 資料庫 schema（DDL/DML/ER） | 資料表、型別、constraint、index 屬 SD（ADR-002） | SD / `docs/db/` |
| 併發控制實作細節 | 屬風控實作細節（ADR-003、risk-control.md），本文件只定義業務語意 | SD |
| 部署 / 金流 / 發券整合 / 前端 | 屬其他服務或專案（`FR-*` Out of Scope 章節、ADR-008） | 各自 spec |

---

## 附錄 A：需求追溯矩陣 (Traceability Matrix)

| FR ID | 需求摘要 | 由 UC 實現 | 由 AC 驗證 |
|-------|----------|-----------|------------|
| FR-CAMP-01 | 活動 CRUD + 狀態機（DRAFT/ACTIVE/ENDED） | UC-1, UC-3 | AC-CAMP-005 |
| FR-CAMP-02 | 多獎品配置（名稱/數量/機率） | UC-2 | AC-CAMP-001/002/003 |
| FR-CAMP-03 | 銘謝惠顧建模為 `THANK_YOU` 獎品 | UC-2 | AC-CAMP-003 |
| FR-CAMP-04 | 機率總和 = 100% 驗證 | UC-2 | AC-CAMP-001 |
| FR-CAMP-05 | 動態修改獎品（後續抽獎生效） | UC-2 | —（見 UC-2 acceptance intent） |
| FR-CAMP-06 | 機率 `[0,100]` + 至少一個 THANK_YOU | UC-2 | AC-CAMP-002, AC-CAMP-003 |
| FR-CAMP-07 | 單次抽獎 | UC-4 | AC-CAMP-004 |
| FR-CAMP-08 | 批次抽獎 count=N | UC-5 | AC-CAMP-008 |
| FR-CAMP-09 | 並發多個單次請求 | UC-4 | AC-CAMP-011 |
| FR-CAMP-10 | 權重隨機演算法 | UC-4, UC-5 | AC-CAMP-004, AC-CAMP-015 |
| FR-CAMP-11 | 活動期間總額次數上限 | UC-1, UC-4 | AC-CAMP-006, AC-CAMP-007 |
| FR-CAMP-12 | 抽獎前檢查剩餘次數、超限 429 | UC-4, UC-5 | AC-CAMP-006 |
| FR-CAMP-13 | 防止重複抽獎（同一請求只一次結果） | UC-4, UC-5 | AC-CAMP-012 |
| FR-CAMP-14 | replay 回傳原結果、不重抽/扣/計 | UC-4, UC-5 | AC-CAMP-012, AC-CAMP-013 |
| FR-CAMP-15 | 批次一次扣 N 次（僅成功計次） | UC-5 | AC-CAMP-009, AC-CAMP-010 |
| FR-CAMP-17 | 中獎通知庫存扣減 | UC-4, UC-5 | AC-CAMP-016 |
| FR-CAMP-18 | 抽中獎品前確認庫存足夠 | UC-4, UC-5 | AC-CAMP-004, AC-CAMP-008 |
| FR-CAMP-19 | 庫存不足降級銘謝惠顧（不重抽） | UC-4, UC-5 | AC-CAMP-014 |
| FR-X-01 | 錯誤流程與輸入驗證（400/401/403/404/409/429/500） | UC-1~UC-5 | AC-CAMP-001/002/003/005/006/010 |
| FR-X-02 | 批次副作用僅執行一次 | UC-5 | AC-CAMP-013 |
| FR-X-03 | 前後端分離、RESTful | —（屬 SD，本文件 Out of Scope） | — |
| FR-X-04 | OpenAPI 3.0 文件 | —（屬 SD，本文件 Out of Scope） | — |
| NFR-03（引用） | 高併發一致性（加速層 + 真相層） | UC-4, UC-5（語意） | AC-CAMP-011/012/014 |
| NFR-07（引用） | 機率分布 / 邊界 / 錯誤測試 | — | AC-CAMP-015 |
