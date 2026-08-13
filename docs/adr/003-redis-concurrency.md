# ADR-003: Redis 併發控制機制 (Redis for Concurrency Control)

**Date:** 2026-08-13
**Status:** Accepted

## Context

抽獎系統的兩大風控目標都需要**跨執行緒 / 跨節點**的原子性操作：

1. **防重複抽獎（Anti-Double-Draw）**：同一使用者對同一活動的同一請求必須只執行一次（見 ADR-005）。
2. **防超抽（Anti-Overselling）**：熱門獎品的庫存被大量併發扣減時不能扣成負數（見 ADR-006）。

分散式環境下，單機的 `synchronized` / JVM lock 無法跨節點生效；DB 層的 `SELECT ... FOR UPDATE` 能保證一致性但會把高併發熱點全壓在 DB 上，延遲高且容易死鎖。需要一個 **低延遲、具備原子原語、且支援跨節點** 的中介層。

## Decision

採用 **Redis 作為併發控制層**，具體使用兩種機制，搭配定義明確的 key schema：

### 1. 分散式鎖：Redlock

- 使用 **Redisson**（Spring Boot 生態的 Redlock client）提供分散式鎖。
- 鎖用於**冪等請求去重**（draw 請求的 idempotency lock，見 ADR-005）與**活動內個人抽獎次數上限的判定臨界區**。
- 每個鎖都帶 **TTL**（lease time），防止持有者當機造成死鎖。

### 2. 原子性操作：Lua Script

- 庫存預扣（stock pre-deduction）與計數器增減使用 **Redis Lua script** 包裝成單一原子步驟（如 `DECR` 前先檢查 `> 0`）。
- 避免「先 GET 再 DECR」的 race condition（read-check-act 競態）。

### Redis Key Schema

| Key 範例 | Type | 用途 | TTL |
|----------|------|------|-----|
| `lock:draw:{userId}:{campaignId}:{idemKey}` | String (SETNX) | 抽獎請求冪等鎖，防同一請求併發重入 | 短 TTL（如 30s） |
| `stock:{prizeId}` | String (counter) | 熱點庫存計數器，供 Lua 原子預扣 | 長期（對齊活動週期） |
| `draw_count:{userId}:{campaignId}` | String (counter) | 個人於活動期間的抽獎次數計數（INCR + EXPIRE） | 至活動結束 |
| `rate:{clientIp}` / `rate:{userId}` | String (counter) | Gateway 限流計數器（配合 INCR + 滑動窗口） | 秒/分級窗口 |

> `{userId}`、`{campaignId}` 以實際 ID 取代；idempotency key 使用 client 提供的 UUID（見 ADR-005）。以 `{...}` 括號相同前綴可確保 Redis Cluster 下 key 落在同一個 hash slot，保證 Lua script 在 cluster 模式也可原子執行。

## Consequences

**正面：**

- **低延遲**：Redis 是 in-memory 操作，遠快於 DB lock；熱點庫存扣減的 p99 延遲可控制在個位數 ms。
- **跨節點安全**：Redlock + Lua 在 service 水平擴展（多 instance）後依然正確。
- **單一 Key 模板**：key schema 集中定義，營運與除錯（`redis-cli keys` / Memorystore 監控）可預期。

**負面 / 需付出的代價：**

- **Redis 成為新的依賴與故障點**：Redis 掛了，抽獎/庫存扣減會直接失敗。需仰賴 Memorystore 的 HA（主從 + 自動 failover，見 ADR-008）。
- **Redlock 有理論爭議**：在極端時鐘/GC pause 場景下 Redlock 並非絕對安全。本系統用 TTL 極短的鎖 + DB UNIQUE constraint 作為最終防線（見 ADR-005），接受理論風險。
- **Redis 資料可能遺失**：Redis 是 cache-like 儲存，`stock:{prizeId}` 只是「預扣的加速層」，**DB 才是庫存真相來源（source of truth）**（見 ADR-006）。Redis 資料遺失可由 DB 重建，不影響最終一致性。
- **額外運維**：需監控 memory usage、eviction policy（建議 `noeviction`，避免 key 被偷 evict 造成風控失效）、連線池與延遲。

## Alternatives

- **純 DB `SELECT ... FOR UPDATE` + 事務**：正確性最佳，但所有熱點併發壓在 DB，throughput 天花板低，POC 驗證「高併發抽獎」的目標無法達成，否決。
- **ZooKeeper / etcd 分散式鎖**：正確性模型更強，但引入額外基礎設施，且不提供「原子計數器 + Lua」的通用能力，對本專案過重，否決。
- **應用層樂觀鎖（DB version column）**：可用於寫回 DB 的條件更新，但無法單獨達成低延遲的熱點預扣與限流計數，作為 DB 層輔助（conditional update）保留、而非替代 Redis。
