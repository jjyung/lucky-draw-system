# 風控與併發控制設計 (Risk Control & Concurrency Design)

> 本文詳細展開抽獎系統的併發控制設計：Redis key schema、Lua script、冪等語意、限流與 Redis/DB 間的最終一致性模型。對應 ADR-003、ADR-005、ADR-006。

## 1. Redis Key Schema

| Key 範例 | Type | TTL | 用途 | 持有者 / 操作 |
|----------|------|-----|------|----------------|
| `lock:draw:{userId}:{campaignId}:{idemKey}` | String (`SETNX`) | 30s | 抽獎請求冪等鎖（同請求併發去重） | Campaign Service: `SET NX PX` |
| `stock:{prizeId}` | String (integer counter) | 對齊活動週期 | 熱點庫存預扣計數器（Redis 加速層） | Campaign Lua 預扣 / Inventory 補償修正 |
| `draw_count:{userId}:{campaignId}` | String (integer counter) | 至活動結束 (`EXPIRE`) | 個人於活動期間的抽獎次數計數（活動期間總額上限） | Campaign: `INCR` + 檢查 |
| `rate:user:{userId}` | String (counter) | 秒/分窗口 | Gateway 使用者層級限流 | Gateway: `INCR` + `EXPIRE` |
| `rate:ip:{clientIp}` | String (counter) | 秒/分窗口 | Gateway IP 層級限流（防刷） | Gateway |
| `jwt:blacklist:{jti}` | String | 至 token 過期 | JWT 主動撤銷黑名單（未來擴充，ADR-009） | Auth Service |

> **Cluster 注意**：key 統一以 `{...}` 包住第一個參數（如 `{userId}`），確保 Redis Cluster 下 Lua script 的所有 key 落在同一 hash slot。本系統的 script 均為單 key 或同前綴多 key，符合要求。

## 2. Lua Script 草稿

### 2.1 庫存預扣 (Stock Pre-Deduction)

```lua
-- KEYS[1] = stock:{prizeId}
-- 回傳 1 = 扣減成功；0 = 庫存不足
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock and stock > 0 then
    redis.call('DECR', KEYS[1])
    return 1
else
    return 0
end
```

**為何原子？** `GET` + `DECR` 若分開執行，兩個請求可能同時讀到 `stock=1`，各自 DECR 造成 -1（超抽）。Lua script 在 Redis 內單線程原子執行，杜絕 read-check-act 競態。

### 2.2 抽獎次數計數 + 檢查 (Draw Count INCR + Check)

```lua
-- KEYS[1] = draw_count:{userId}:{campaignId}
-- ARGV[1] = 活動允許的期間總額上限
-- ARGV[2] = 活動剩餘秒數（至活動結束，用於 TTL）
local count = tonumber(redis.call('INCR', KEYS[1]))
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])  -- 首次計數時設定 TTL 至活動結束
end
if count > tonumber(ARGV[1]) then
    redis.call('DECR', KEYS[1])  -- 超限回滾計數
    return 0
end
return 1
```

### 2.3 冪等鎖 (Idempotency Lock)

```bash
SET lock:draw:{userId}:{campaignId}:{idemKey} "1" NX PX 30000
# 回傳 OK → 取得鎖；nil → 已有人在處理
```

- 釋放使用 Lua 保證「只有持有者能釋放」（比對 value 再 `DEL`），避免誤刪他人鎖：

```lua
-- KEYS[1] = lock key, KEYS[2] = owner token, ARGV[1] = value
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
```

## 3. 冪等語意 (Idempotency Semantics)

### 3.1 判定矩陣

| 情境 | Redis 鎖 | DB draw_record | 系統行為 | HTTP |
|------|----------|----------------|----------|------|
| 全新請求 | 取得 | 無 | 執行抽獎，落庫 | 200 + 新結果 |
| 併發重入（同 key 同時到） | 取不到 | 無（尚未落庫） | 回請重試 | 409 |
| Replay（同 key 處理完成後再來） | 取得（鎖已釋放） | 有 | 回傳既有結果快照，**不重抽** | 200 + 原結果 |
| Replay（鎖過期但 DB 有） | 取得 | 有 | 同上（DB 查詢先於抽獎） | 200 + 原結果 |
| 極端：鎖過期且 DB 無（執行者當機） | 取得 | 無 | 視為新請求重抽 | 200 + 新結果（可接受：原請求從未完成） |

> **關鍵原則**：DB 的 `UNIQUE(user_id, campaign_id, idempotency_key)` 是唯一絕對防線；Redis 鎖只決定「誰先執行」。兩者之間可能出現的縫隙（鎖過期 + 執行者未完成）由 DB constraint 兜底——後執行者撞 UNIQUE，轉為查表回傳原結果。

### 3.2 落庫與衝突處理

```sql
-- 抽獎結果落庫（Campaign DB）
INSERT INTO draw_records (user_id, campaign_id, idempotency_key, prize_id, result_type, payload_json)
VALUES (?, ?, ?, ?, ?, ?);

-- 撞 UNIQUE 衝突 → 例外處理
-- 1) SELECT 既有記錄
-- 2) 回傳該記錄的結果快照 (replay)
-- 3) 不執行任何扣減/計數副作用
```

### 3.3 副作用清單（僅執行一次的動作）

1. 權重抽選本身（不重抽）。
2. `draw_count` INCR（不重複計次）。
3. `stock:{prizeId}` Lua 預扣（不重複預扣）。
4. `inventory-commit` event 發布（consumer 以 `drawRecordId` 冪等去重）。

## 4. 限流 (Rate Limiting)

### 4.1 兩層限流

| 層級 | 位置 | 標的 | 做法 |
|------|------|------|------|
| L1 — 粗粒度 | API Gateway | 每 user / 每 IP | 固定窗口計數（`INCR` + `EXPIRE`），超限回 `429`。保護上游服務與 Redis 本身 |
| L2 — 業務語意 | Campaign Service | 每 user 每活動期間總額抽獎次數 | `draw_count` counter + 活動設定上限，超限回 `429`。這是**營運規則**，不是防刷 |

### 4.2 固定窗口示意

```text
rate:user:{userId}:minute
  值 = INCR 的結果；第一次 INCR 時 EXPIRE 60s
  超過閾值 (如 30 req/min) → 429
```

- 固定窗口在窗口邊界有 2 倍 burst 的理論缺口；對 POC 足夠，若 prod 需要精確可換滑動窗口（sorted set / 或簡化的多窗口計數）。

## 5. Redis 與 DB 的最終一致性模型 (Eventual Consistency)

### 5.1 角色定位

| 儲存 | 角色 | 一致性角色 |
|------|------|-----------|
| Redis `stock:{prizeId}` | **加速層**：低延遲預扣、併發判定「還有沒有貨」 | 可暫時不精確、可重建 |
| Inventory DB `inventory.stock` | **真相來源 (source of truth)**：出貨扣減、賠償判定 | 必須精確、不可超扣 |

### 5.2 時間線與不一致窗口

```text
T0  Campaign Lua 預扣 stock:{prizeId} 50→49   (Redis)
T1  Campaign 落庫 draw_record (result_type=WIN)
T2  Campaign 發布 inventory-commit ──────────► Inventory Service
T3  Inventory 條件更新 DB stock 50→49          (DB 真相)
                    │
                    └── T0~T3 之間：Redis=49、DB=50（不一致窗口）
                        此時查「剩幾件」：Redis 說 49，DB 說 50
```

- **不一致窗口大小** ≈ 消息延遲 + consumer 處理時間（通常 ms ~ 秒級）。
- **方向性**：Redis 只可能「比 DB 少」（預扣先行），不可能「比 DB 多」——除非發生補償回滾（見 Path C）。這保證「DB 真相」永遠不超扣。

### 5.3 收斂機制 (Convergence)

1. **正常收斂**：每個 inventory-commit 的條件更新讓 DB 追平 Redis。
2. **異常補償**：DB 更新 0 列 → 回滾中獎結果、修正 Redis counter（把誤扣的額度加回）。
3. **定期對帳 job**（建議 cron，活動期間每 5 分鐘）：
   - 以 Inventory DB 剩餘庫存為基準，校正 Redis `stock:{prizeId}`；
   - 掃描 `reservations` 表中超過預留 TTL 未 commit 的記錄 → 回收 Redis 額度。
4. **活動結束清算**：以 DB 最終庫存 + draw_records 對帳產出營運報表。

### 5.4 為什麼可以接受 eventual consistency？

- 抽獎是「短暫高併發、後台寫回」的場景，client 不需要即時看到「庫存剩 N 件」的精確值。
- 唯一不可妥協的約束是「**發出的實體獎品 ≤ DB 庫存**」——這由 DB 條件更新保證，與 Redis 的即時性無關。

## 6. 併發安全總表 (Concurrency Safety Summary)

| 風險 | 防線 | 失效後的兜底 |
|------|------|-------------|
| 同請求重複執行 | Redis SETNX 冪等鎖 (TTL 30s) | DB UNIQUE(user_id, campaign_id, idempotency_key) |
| 庫存超扣 | Lua 原子預扣（Redis 加速層） | DB `UPDATE ... WHERE stock > 0`（真相層） |
| 次數超抽（user 繞過限流） | `draw_count` counter + 上限檢查 | 活動結束對帳稽核 |
| Redis 資料遺失 | — | 由 DB 重建 counter（對帳 job） |
| 消息重複投遞 | consumer 以 `drawRecordId` 冪等去重 | DB UNIQUE（reservations） |
| Redlock 理論失效（GC/時鐘） | 短 TTL + 執行預算 | DB UNIQUE / 條件更新（最終仍正確） |
