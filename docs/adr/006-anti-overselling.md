# ADR-006: 防超抽機制 (Anti-Overselling)

**Date:** 2026-08-13
**Status:** Accepted

## Context

熱門獎品（如 iPhone、限量券）會在活動開放瞬間被大量併發抽獎請求擊中。**防超抽** 的目標是：無論多少併發，**實際發放的實體獎品數絕不能超過庫存**。

難點：

- 純 DB 條件更新（`UPDATE inventory SET stock = stock - 1 WHERE id = ? AND stock > 0`）可以保證不超抽，但熱點行會成為 DB 的**鎖熱點**，throughput 低、延遲高。
- 純 Redis 計數器扣減延遲低，但 Redis 是 volatile / 可能遺失資料的儲存，不能當唯一真相來源。
- 抽獎流程中「抽到實體獎品」與「庫存扣減」分屬不同 service（campaign vs inventory，見 ADR-002），需要跨 service 的一致性設計。

## Decision

採用 **「Redis 原子預扣（前置加速層）＋ Kafka event 異步觸發 DB 條件更新（最終真相來源）」** 的兩段式設計：

### 第一段：Redis Lua 原子預扣（熱點路徑）

庫存預扣透過 Redis Lua script 一次完成「檢查 + 扣減」：

```lua
-- KEYS[1] = stock:{prizeId}
-- 回傳值：1 = 扣減成功；0 = 庫存不足
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock > 0 then
    redis.call('DECR', KEYS[1])
    return 1
else
    return 0
end
```

- 此 script **原子執行**，高併發下也不會有兩個請求同時看到 `stock > 0` 而超扣（見 ADR-003）。
- Redis 扣減成功 → 該次抽獎「暫時鎖定」一件庫存（pre-deduction / reservation）。

### 第二段：DB 條件更新（真相來源）

- campaign-service 抽中實體獎品後，發布 **Kafka event：`inventory-commit`**（見 ADR-007），由 **inventory-service 的 async consumer** 消費。
- consumer 執行**原子條件更新**（database-per-service，inventory DB）：

```sql
UPDATE inventory SET stock = stock - 1 WHERE id = ? AND stock > 0
-- 影響 row count = 1 → commit 成功
-- 影響 row count = 0 → 庫存已被 Redis 期間的其他人清空（異常路徑，見下）
```

- **DB 是庫存唯一真相來源**。Redis 的 `stock:{prizeId}` 只是加速層，可隨時由 DB 重建；Redis 與 DB 短暫不一致是可接受的（eventual consistency，見 `docs/architecture/risk-control.md`）。

### 失敗路徑與補償

| 情境 | 處理 |
|------|------|
| Redis 預扣回傳 0（庫存不足） | 抽獎結果**降級為銘謝惠顧（THANK_YOU）**，不發布 inventory-commit；client 拿到「未中獎」結果 |
| `inventory-commit` 消費後 DB 條件更新影響 0 列（Redis 與 DB 帳面不一致） | **補償（compensation）**：inventory-service 記錄異常、回滾該次中獎結果（如將 draw_record 標記為 VOID / 產生 `draw_reversal`），並發出 alert；後續以 DB 為準修正 Redis 計數器 |
| consumer 當機 / event 遺失 | Kafka 的 at-least-once + consumer group offset 確保 event 最終被處理；consumer 需要**冪等**（依 `draw_record_id` 去重，避免重複扣 DB） |
| 活動結束清理 | 由 DB 剩餘庫存 + 預留記錄（`reservations`）對帳，校正 Redis 計數器 |

### 計數與扣減語意

- **Redis 預扣 ≠ 最終扣減**：Redis 預扣是「暫時保留」，DB 條件更新才是「真正出貨」。若 DB 更新失敗，預扣的 Redis 額度在對帳時回收。
- 每個實體獎品中獎都有唯一 `draw_record_id` 作為 event 的冪等鍵（見 ADR-005）。

## Consequences

**正面：**

- **低延遲 + 不超抽**：熱點扣減在 Redis 完成（ms 級），DB 真相來源用原子條件更新保證不會扣成負數，兩者皆不違反「絕不超抽」。
- **DB 不被熱點壓垮**：`inventory-commit` 是 async batch，DB 寫入平滑化。
- **真相來源清晰**：任何時刻問「到底剩幾件」，以 DB 為準。

**負面 / 需付出的代價：**

- **最終一致性（eventual consistency）**：Redis 顯示「還有貨」與 DB 實際出帳之間有時間窗，需靠對帳與補償機制收斂；不能做到讀到的庫存 100% 即時精確。
- **補償邏輯複雜**：DB 條件更新失敗時需要回滾中獎結果 + 修 Redis，是系統中最容易出 bug 的路徑，需要完整測試覆蓋（Testcontainers + integration test）。
- **Kafka/RabbitMQ 成為關鍵路徑**：`inventory-commit` 依賴消息佇列可靠投遞，broker 故障會導致庫存扣減延遲（但不超抽，只是出貨慢），需監控 backlog。
- **Redis 與 DB 對帳**：需要定期對帳 job 將 Redis counter 與 DB 校正一致。

## Alternatives

- **純 DB 同步扣減（每抽一次就鎖行更新）**：正確性最強，但熱點行 lock contention 使 QPS 天花板過低，抽獎活動的峰值需求無法滿足，否決。
- **純 Redis 扣減、DB 只做最終記帳（不條件更新）**：Redis 資料可能遺失（當機/eviction），無法保證 DB 不超抽，違反核心目標，否決。
- **預先扣光再發放（庫存全部預先保留）**：管理簡單但庫存利用率低，且無法處理「中獎者放棄」等回補情境，否決。
