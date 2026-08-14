# ADR-005: 防重複抽獎與冪等性 (Anti-Double-Draw & Idempotency)

**Date:** 2026-08-13
**Updated:** 2026-08-14 — 抽獎端點統一 `/api/v1` 前綴；首次抽獎與 replay 統一回傳 `200 OK`。
**Status:** Accepted

## Context

抽獎是高價值、非冪等的商業動作，但網路層天然不可靠：

- **Client 重試**：Timeout 後 client 不知道請求是否成功，會重送同一筆請求。
- **併發重入**：使用者連點、或 mobile app 同時發出兩次請求。
- **重複扣次數 / 重複出獎**：同一使用者因重送而抽到兩次獎、被扣兩次抽獎次數，是營運上不可接受的事故。

REST `POST /api/v1/campaigns/{id}/draw` 需要一個讓 client 可以安全重試的機制。常見解法是 **Idempotency-Key**（如 Stripe 的 pattern）。

## Decision

採用 **Idempotency-Key 機制**，由三層構成，互相補位：

### 1. Client 端：Idempotency-Key Header

- Client 在每次 `POST /api/v1/campaigns/{id}/draw` 時必須提供 **`Idempotency-Key: <UUID>`** header。
- 同一「物理動作」（一次使用者點擊）的所有重試使用**同一個 UUID**；新的一次點擊使用新 UUID。
- Gateway 對缺少 header 的 draw 請求回傳 `400 Bad Request`。

### 2. 複合冪等鍵 (Composite Idempotency)

系統層級的冪等識別為 **`userId + campaignId + idempotencyKey`** 三者的組合：

- 只靠 UUID 不夠（惡意/錯誤 reuse 可能跨使用者重放）；
- 只靠 `userId + campaignId` 不夠（同使用者同活動多次合法抽獎會被誤判為重複）。

### 3. 兩道防線：Redis 鎖（第一線）＋ DB UNIQUE（最終保證）

**第一線 — Redis SETNX 鎖（短 TTL）：**

```lua
-- 若鎖不存在則取得並設定 TTL；存在則回傳 0
SET lock:draw:{userId}:{campaignId}:{idemKey} "1" NX PX 30000
```

- 併發的相同請求：只有一個能拿到鎖，其餘直接回傳 **`409 Conflict`**（或短暫等待後重試）。
- 拿到鎖者執行完整抽獎流程（權重抽選 → 庫存預扣 → 落庫，見 ADR-004 / 006）。
- 鎖帶 30s TTL，防止執行者當機造成死鎖。

**第二線 — DB 層 UNIQUE constraint（最終保證）：**

```sql
ALTER TABLE draw_records
  ADD CONSTRAINT uq_draw_idem UNIQUE (user_id, campaign_id, idempotency_key);
```

- 即使 Redis 鎖因時鐘漂移 / GC pause / TTL 過期而失效，DB 的 UNIQUE constraint 保證**同一複合鍵最多只有一筆 draw_record**。
- 落庫時若撞 UNIQUE → 代表該請求已處理過 → **replay 路徑**（見下）。

### 4. Replay 語意：回傳原始結果

- 抽獎成功後，**draw_record 中保存結果快照**（獎品 ID、獎品名稱、結果型別），並以 `Idempotency-Key` 為索引可查。
- 當收到相同複合鍵的請求且鎖不存在（代表已處理完成）→ 查詢既有 `draw_record`，**回傳與第一次完全相同的結果**（HTTP 200 + 相同 response body）。抽獎成功一律回傳 `200 OK`（首次與 replay 皆 200），replay 的 body 與首次完全一致，client 無法以 status 區分首次與重放。
- **不重抽、不重扣庫存、不重扣次數**。對 client 而言，replay 是透明的「同一個結果」。

### 5. 個人抽獎次數上限的判定

- 每次抽獎前檢查 `draw_count:{userId}:{campaignId}`（Redis 計數器，見 ADR-003）是否已達活動期間總額上限。
- 計數在「抽獎成功確定後」才 INCR（僅實際產生結果的請求計次；重複請求 / replay 不重複計次）。

## Consequences

**正面：**

- **重試安全**：client 可以放心 timeout 後重送，不會造成重複出獎或重複扣次。
- **雙層防線**：Redis 保證併發下「只有一個執行者」，DB UNIQUE 保證極端情境下也無法重複落庫——防線有冗餘，單層失誤不造成事故。
- **營運可稽核**：`draw_records.idempotency_key` 讓客服/稽核可追蹤「一次點擊 → 一筆結果」。

**負面 / 需付出的代價：**

- **Client 義務**：Client 必須正確管理 Idempotency-Key 生命週期（一次點擊一個 UUID）；為降低門檻，SDK/前端範例需內建此邏輯。
- **Redis 鎖 TTL 的權衡**：TTL 太短 → 慢請求在完成前鎖就過期，可能被第二個請求趁隙重入（靠 DB 防線補救）；TTL 太長 → 執行者當機後請求卡在鎖上直到過期。實作上需對鎖內工作設定明確的執行時間預算，並搭配 watchdog/續約（Redisson 預設提供）。
- **replay 需要快照查詢**：draw_record 需保留足夠的結果欄位，避免 replay 時需要重新組合 response。

## Alternatives

- **僅靠 Redis 鎖、不設 DB UNIQUE**：Redis 是可能遺失資料的 cache-like 儲存，且 Redlock 有理論失效情境，單靠它保證不了「絕不重複」，否決。
- **僅靠 DB UNIQUE、不用 Redis 鎖**：正確但每個重複請求都會打到 DB（並行時其中一個撞 constraint 噴錯，需額外處理），熱點請求放大 DB 壓力，延遲高，否決。
- **Client 自行保證（要求 client 先去重）**：把正確性交給 client 等於沒有保證，電商風控不能接受，否決。
- **以 `userId + campaignId + date` 為冪等鍵（每日一次抽獎）**：無法支援「同活動多次抽獎」的營運需求，否決。
