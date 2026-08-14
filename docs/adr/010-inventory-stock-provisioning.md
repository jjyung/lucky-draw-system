# ADR-010: 庫存初始與配置同步 (Inventory Stock Provisioning)

**Date:** 2026-08-14
**Status:** Accepted

## Context

天條要求「獎品數量（庫存）可動態配置修改」（`requirements.md` §0、`FR-CAMP-05`），而 inventory-service 是「庫存真相來源」（ADR-006），其庫存初始值來自 campaign-service 的 `prize.quantity`（`FR-CAMP-02`）。此處有兩個未解的張力：

1. **初始值如何進入 inventory**：inventory SA §5.1 把「初始值如何進入本服務」標為「屬 SD 層，未設計」；現有種子資料（`dml-seed.md` §4）只涵蓋 dev 初始同步，未定義 runtime 機制。
2. **動態修改 vs 只扣減**：inventory SA §7 把「補貨／庫存追加」列為 Out of Scope，並在 §4.4 宣稱「出貨真相扣減為單向（只扣不加）」；這與天條「數量可動態修改」、以及 campaign SA UC-2「獎品內容（名稱、數量、機率）可動態修改，修改後於後續抽獎生效」相牴觸。

硬約束：**ADR-002** 禁止跨 DB 存取（方案 C 直接出局）；**ADR-007** 已建立 Spring Cloud Stream + RabbitMQ（bindings：`draw-result`、`inventory-commit`，payload 以 `draw_record_id` 為冪等鍵）；**ADR-006** 以條件更新 `UPDATE ... WHERE stock > 0` 保證「實發 ≤ 庫存」。

## Decision

### Q1：採用「事件驅動」同步（方案 A）

campaign-service 在獎品建立或 `quantity` 修改時，透過既有 Spring Cloud Stream（ADR-007）發布新 binding **`prize-stock-configured`**，inventory-service 消費後以 `prize_id` 為鍵做**冪等 upsert**。`THANK_YOU` 獎品不發布（銘謝惠顧不扣庫存，`stock` 忽略）。

Event payload（共用 DTO，放 `common` module）：

- `prizeId`（upsert／冪等鍵）、`campaignId`（路由與稽核）
- `oldQuantity`、`newQuantity`（campaign 於更新交易內原子取得 old 值，是 config 真相）
- `configVersion`（每獎品單調遞增，用於冪等去重與排序）

**為什麼選 A**：重用 ADR-007 既有 broker/binder，無新基礎設施；符合 ADR-002（無跨 DB 存取）；非同步解耦，campaign 不因 inventory 故障而配置失敗（與 ADR-007 否決同步 HTTP 的理由一致）；`prize_id` 為自然鍵，`uq_inventory_prize_id` 唯一索引可承接 upsert。

**否決 B（內部 REST）**：同步呼叫使 campaign 建立/改獎品路徑與 inventory availability 耦合，inventory 故障會讓 ADMIN 的配置操作失敗；且 inventory SA §2.2 定位「無 client 可直接呼叫之狀態變更介面」，內部 REST 端點與此定位衝突，需額外擴面。
**否決 C（直接讀 campaign DB）**：違反 ADR-002，直接出局。

### Q2：採用「差值調整 + 下限 guard」語意（方案 B）

任何活動狀態下，`quantity` 修改都連動庫存。inventory 收到 event 後以 `delta = newQuantity − oldQuantity` 更新剩餘庫存：

```sql
UPDATE inventory
   SET stock = stock + :delta, version = version + 1, updated_at = now()
 WHERE prize_id = :prizeId
   AND stock + :delta >= 0;
-- rowcount = 1 → 套用成功
-- rowcount = 0 → 新總量 < 已發放數 → 拒絕，不套用
```

consumer 處理流程：先以 `configVersion` 做冪等/排序檢查 → 若 inventory 列不存在（首次建置）則 `INSERT`（`stock = newQuantity`、`last_config_version = v`）→ 若存在則執行上述條件更新。

- **payload 帶 old/new 而非 delta**：campaign 是 config 真相、於更新交易內知曉 old 值，delta 由 inventory 推導。帶絕對值使 event 可稽核、可重放；單帶 delta 在重複投遞／亂序下會重複套用，脆弱。
- **下限 guard = 「不得低於已發放數」**：因 `stock = oldQuantity − issued`（已發放數），`stock + delta = newQuantity − issued`，故 `stock + delta >= 0` 等價於 `newQuantity >= issued`，直接封住「新總量小於已發放數」。現有 `CHECK (stock >= 0)`（inventory-db.md）為最後一道 DB 防線。
- **「新總量 < 已發放數」→ 拒絕（不 clamp）**：clamp 會悄悄改寫 ADMIN 的配置意圖、掩蓋錯誤；拒絕則保留 `stock` 不變、記錄衝突並觸發告警（`NFR-06`），由營運決定調高 quantity 或接受。因非同步，campaign 側 config 已先行持久化，會產生 config 與 inventory 的分歧，由對帳收斂（見 Consequences）。
- **冪等與排序**：inventory 列新增 `last_config_version`；consumer 僅在 `incomingVersion > last_config_version` 時套用。相同版本重投遞 → 跳過（at-least-once 冪等，對齊 ADR-007）；較低版本 → 視為亂序/過期，跳過。
- **增加 = 補貨、減少 = 縮減**：兩者皆經由「獎品 quantity 配置修改」達成，inventory **不另設補貨 API**。

**為什麼選 B**：唯一能同時滿足天條「數量可動態修改」（`FR-CAMP-05`）與 `FR-INV-02`「實發絕不超配置」的選項。config 路徑與扣減路徑共用同一條件更新機制（加法與減法對同一列可交換，PostgreSQL 行鎖序列化，無 lost update），不需第二套併發設計。

**否決 Q2-A（僅 DRAFT 可改、ACTIVE 凍結）**：最簡單、與「不補貨」最一致，但直接違反天條與 `FR-CAMP-05`（Must），不得採用。
**否決 Q2-C（總量覆寫 + 由 reservations 重算剩餘）**：需新增 `total` 欄位或每次重算 issued，重且侵入既有 schema；覆寫式語意也無法自然表達「增加」與「減少」的差別，收益不抵成本。

## Consequences

**正面：**

- 打通「初始值進入 inventory」的 runtime 機制；種子資料之外，實際獎品建立/修改即自動同步。
- 同時滿足「動態改數量」與「絕不超抽」；config 與扣減共用條件更新，無新增基礎設施。

**負面／需付出的代價：**

- **放寬 inventory SA「不補貨」Out of Scope**：補貨現在可經由「quantity 增加」達成。這是 business-semantics 變更，需回寫 inventory SA §7、§4.4、§5.1（見下方待辦）；「扣減單向」仍成立，但「庫存只減不增」不再成立。
- **最終一致性**：config event 到 inventory 套用之間有窗口。方向性保守：**增加**延遲套用 → 暫時少發（可接受）；**減少**延遲套用 → 短暫仍可發到舊總量，但受「已同步庫存」上限與 DB 條件更新約束，絕不超發，窗口通常 ms~s。
- **config vs inventory 分歧**：減少被拒時（`new < issued`），campaign 已存新 quantity、inventory 未變，產生分歧，須靠對帳 job（`FR-INV-05` 延伸：比較 campaign `quantity` 與 inventory `issued + stock`）偵測並告警，由營運決策。
- **冪等/排序依賴 `configVersion`**：RabbitMQ 單 queue FIFO 在 POC 大致保序，但換 Kafka binder 時必須以 `prize_id` 為 partition key 保序（呼應 ADR-007「換 binder 需重新驗證消費順序」）；亂序/漏訊造成的 delta 漂移由對帳收斂。
- **需新增欄位**：`inventory.last_config_version`（冪等/排序）；DB 註解「只能扣減」需修正。

## Alternatives

- Q1-(B) 內部 REST API：同步耦合、擴面且與 SA 定位衝突，否決。
- Q1-(C) inventory 直讀 campaign DB：違反 ADR-002，否決。
- Q2-(A) 僅 DRAFT 可改、ACTIVE 凍結：違反天條 `FR-CAMP-05`，否決。
- Q2-(C) 總量覆寫 + reservations 重算剩餘：較重、侵入 schema，否決。
