# 單元測試規約 (Unit Testing)

> 來源：[AI 時代的自動化測試與驗收（二）：Unit Test](https://tech.samsonlab.dev/blog/ai-era-testing-and-acceptance-unit-test/)（Samson，2026-08-13）。本文為其**實作濃縮**，供 Backend 與 AI agent 寫測試時對照。原始論述與引註見原文。

## 1. 核心定位：unit test 保護的是「不變量」，不是 coverage

- **unit = 單一行為單元（unit of behavior）**，不是「一個方法」。測試單位可以是方法、一組協作物件、甚至一個服務，重點是「一個可獨立驗證的行為」。
- **寫測試前先答一題**：「如果這條規則被改錯，哪個業務會受害？」測試就該保護那個。若一個測試刪掉也不痛、改壞也抓不到業務問題，它保護的只是 coverage 數字——**刪掉**。
- 值得保護的典型不變量：金額不可為負、庫存不可超賣、退款後不可回已出貨、機率總和必須 = 100%、狀態機終態不可回轉、冪等鍵重送不可重複副作用。

## 2. Mock 的取捨（預設 classical，少用 Mock）

| 情境 | 做法 |
|------|------|
| 純 domain 邏輯（規則、計算、狀態機） | **不 mock**，直接測真邏輯。若需 mock DB/HTTP/MQ 才能測，多半是 domain 邏輯與基礎設施纏在一起 → **先重構分離**，不是補 mock |
| 依賴回傳 X 時，邏輯決策對不對 | 用 **Stub** 給固定答案 + **state verification**（驗「結果對不對」）✅ |
| 某 method 被呼叫幾次、照什麼順序 | **Mock + behavior verification**（驗「有沒有照假設方式呼叫」）❌ 鎖死實作，重構即碎 |
| 時間、亂數、外部 client 回傳 | 值得替換（Stub/Fake），因不可控 |

- **警訊**：需要 mock「受測 class 自己的 method」或「自己團隊寫的 repository」→ 是該重構的信號，不是該補 mock 的信號。
- **預設**：能用 Stub 給固定答案、用 state verification 驗結果，就優先。Mock 留給真的難搞的邊界（如 cache 這類看不到狀態處）。

## 3. AI 生成測試的三個坑（審查用）

1. **鏡射實作**：assert 當時寫法的細節，換一種正確寫法測試就碎。
2. **重述錯誤邏輯**：照 buggy 實作寫測試，把 bug 固定成「正確」，之後沒人敢改。
3. **全 happy path**：只測正常輸入，邊界/異常/非法狀態全漏，coverage 漂亮但出事情境全在漏網。

**三個審查問題**（直接拿去篩 AI 生成的測試）：
- 把實作換成另一種**正確**寫法，這個測試還過嗎？不過 → 它在測實作，不是測行為。
- 如果需求被**誤解**，這個測試抓得到嗎？抓不到 → 它在測細節，不是測規則。
- 這個測試**唯一會紅**的理由，是不是只有「有人改了它的實作」？

## 4. TDD 節奏

- **Red → Green → Refactor**，小步改動配快速測試，壞了才知道是哪一步造成。
- **速度是硬條件**（不是優化選項）：測試快到 agent/開發者願意頻繁跑。
  - **compile suite**（每次想編譯就跑）：秒級。
  - **commit suite**（提交前跑）：十分鐘內（Kent Beck 準則）。
  - unit test 若一次要好幾分鐘，agent 不會等它，測試就會被繞過 → 這是測試在 AI workflow 存活與否的條件。

## 5. 分工：人定義不變量，AI 補案例

1. **人**先把 domain 不變量與關鍵邊界列出來（「哪些規則值得保護」的判斷，AI 從空白猜不準）。
2. **AI** 依不變量補 parameterized cases、邊界值、異常與非法狀態。
3. **人** review，用 §3 三問題過篩，刪掉/改寫鎖實作的測試。

> 把不變量寫清楚這個動作本身，會逼團隊把規則講明——很多問題不是測試少，是規則根本沒被寫出來。**不變量優先從 SA 的 business rule / AC 擷取**（本 repo 的 UC 已列 business rule，測試直接對齊它）。

## 6. 何時不值得寫 unit test

- **CRUD、簡單傳遞層**：unit test 價值很低，硬補是過度設計。那層的風險在**整合與契約**，不在單一規則 → 交給 integration / contract test（見 test-gen 對應層級）。
- **純計算、domain 規則、狀態轉換**：值得寫重，parameterized test 用力套。

## 7. 本 repo 對應

| 測試對象 | 從哪裡抓不變量 | 範例 |
|----------|----------------|------|
| 權重抽獎演算法 | SA UC-4/UC-5 business rule、ADR-004 | 機率區間邊界、總和=100% 收斂、累計區間命中 |
| 防超抽條件更新 | SA inventory UC-1、ADR-006 | `WHERE stock > 0` 不為負、rowcount=0 補償 |
| 冪等/replay | SA UC-4 business rule、ADR-005 | 同鍵重送不重抽/不重扣/不重計 |
| 狀態機 | SA UC-3、campaign §4.1 | `DRAFT→ACTIVE→ENDED`、終態不可回轉 |
| 機率驗證 | SA UC-2、FR-CAMP-04/06 | 總和≠100% 拒絕、越界拒絕、缺 THANK_YOU 拒絕 |

> **核心原則**：好的 unit test 不一定多，也不追求最高 coverage。它讓重要規則容易驗證、讓重構有安全網、讓團隊知道哪些行為一旦改變就必須重新討論。**寫測試之前，先問「我們要保護什麼」。**
