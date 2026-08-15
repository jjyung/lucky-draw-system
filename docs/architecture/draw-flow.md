# 抽獎完整流程 (Draw Flow)

> 本文描述一次抽獎請求從 Client 到最終結果的**完整生命週期**，包含正常路徑與所有失敗路徑。相關決策：ADR-003（Redis 併發）、ADR-004（權重抽獎）、ADR-005（冪等）、ADR-006（防超抽）、ADR-007（消息）、ADR-009（安全）。

## 1. 正常路徑 (Happy Path) — 抽中實體獎品

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant G as API Gateway
    participant CS as Campaign Service
    participant R as Redis
    participant INV as Inventory Service
    participant DB as Campaign DB (draw_records)
    participant MQ as RabbitMQ (Cloud Stream)

    C->>G: POST /campaigns/{id}/draw<br/>Authorization: Bearer JWT<br/>Idempotency-Key: UUID
    G->>G: 驗證 JWT 簽章 (RS256 public key)<br/>檢查 Idempotency-Key 存在<br/>Rate limit (Redis 計數器)
    G->>CS: 轉發 (X-User-Id, X-User-Roles headers)

    CS->>CS: 檢查活動狀態 = ACTIVE<br/>檢查個人抽獎次數上限 (draw_count)
    CS->>R: SETNX lock:draw:{userId}:{campaignId}:{idemKey} NX PX 30000
    alt 鎖取得失敗 (已有人處理)
        CS-->>C: 409 Conflict（並發重入，回請重試）
    end

    CS->>DB: SELECT draw_record<br/>WHERE user_id+campaign_id+idem_key
    alt 已有記錄 (Replay)
        DB-->>CS: 既有 draw_record
        CS-->>C: 200 OK + 原始結果快照 (不重抽)
    end

    CS->>CS: 權重隨機抽選 (單一 random double in [0,100))<br/>累計機率區間 → 命中 p1 (iPhone, 5%)
    CS->>R: Lua 預扣 stock:{prizeId}<br/>GET stock / if >0 DECR
    alt 庫存不足 (Lua 回傳 0)
        CS->>CS: 結果降級為 THANK_YOU (銘謝惠顧)
        CS->>DB: INSERT draw_record (result_type=THANK_YOU, prize_id=NULL, idem_key)
        CS-->>C: 200 OK 銘謝惠顧
    end

    CS->>DB: INSERT draw_record<br/>(result_type=WIN, prize_id, idem_key)
    CS->>R: INCR draw_count:{userId}:{campaignId}<br/>(TTL 對齊活動結束時間)
    CS->>MQ: publish inventory-commit<br/>(drawRecordId, prizeId, quantity=1)
    CS-->>C: 200 OK + 中獎結果 (prize id/name)

    MQ->>INV: consume inventory-commit (async)
    INV->>INV: 冪等檢查 (drawRecordId 去重)
    INV->>DB: UPDATE inventory SET stock = stock - 1<br/>WHERE id = ? AND stock > 0
    alt 影響 1 列
        INV->>INV: commit 成功，回寫 reservations
    else 影響 0 列 (DB 與 Redis 帳面不一致)
        INV->>INV: 補償：reservations.status=REVERSED<br/>觸發 alert，修正 Redis counter
    end
```

### 正常路徑步驟摘要

| Step | 負責者 | 動作 | 關鍵機制 |
|------|--------|------|----------|
| 1–4 | Gateway | JWT 驗證、Idempotency-Key 檢查、限流、路由 | ADR-009、ADR-005 |
| 5 | Campaign | 活動啟用 + 個人次數上限檢查 | `draw_count` Redis counter |
| 6 | Campaign+Redis | 冪等鎖 SETNX (TTL 30s) | ADR-005 |
| 7–9 | Campaign | replay 檢查（撞 UNIQUE 或鎖過期後查表） | ADR-005 |
| 10 | Campaign | 權重隨機抽選（O(n) 累計區間） | ADR-004 |
| 11 | Campaign+Redis | 熱點庫存 Lua 原子預扣 | ADR-003 / 006 |
| 12–13 | Campaign+DB | 落庫 draw_record（含 idempotency_key UNIQUE） | ADR-005 |
| 14 | Campaign+Redis | 抽獎次數 INCR（僅成功請求計次） | ADR-003 |
| 15–17 | Campaign→MQ | 發布 inventory-commit，立即回 client | ADR-007 |
| 18–21 | Inventory | 異步條件更新 DB（source of truth）+ 補償 | ADR-006 |

## 2. 失敗路徑 (Failure Paths)

### Path A — 鎖競爭 / 鎖等待逾時

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant G as API Gateway
    participant CS as Campaign Service
    participant R as Redis

    C->>G: POST draw (Idempotency-Key: X)
    G->>CS: 轉發
    CS->>R: SETNX lock ... idemKey=X
    Note over R: 同 key 請求已在處理<br/>(另一 request 持鎖中)
    R-->>CS: 0 (取得失敗)
    CS->>CS: 重試鎖 1~2 次（短等待），仍失敗
    CS-->>C: 409 Conflict + Retry-After header
    Note over C: Client 以相同 Idempotency-Key 重試
    C->>G: POST draw (Idempotency-Key: X) 重試
```

- **行為**：並發的相同請求回 `409 Conflict`；Client 應以**相同 Idempotency-Key** 重試。
- **防護**：若鎖因 TTL 過期 / 執行者當機而消失，DB 的 UNIQUE constraint 仍是最終防線（重試會走 replay 路徑）。

### Path B — 庫存不足 → 降級銘謝惠顧

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant CS as Campaign Service
    participant R as Redis
    participant DB as Campaign DB

    CS->>CS: 權重抽選命中實體獎品 p1
    CS->>R: Lua 預扣 stock:{prizeId}
    alt stock = 0
        R-->>CS: 0 (庫存不足)
        CS->>DB: INSERT draw_record (type=THANK_YOU)
        CS-->>C: 200 OK 銘謝惠顧 (未中獎)
        Note over CS: 不發布 inventory-commit，不扣次數外的任何東西<br/>draw_count 仍計 1 次（本次請求確實執行了抽獎）
    end
```

- **語意**：中獎機率是「配置機率」，庫存不足時**不重抽**，直接降級為銘謝惠顧。這是刻意設計——避免「抽中後庫存不足再重抽」造成機率被機率地改寫與營運上的不公平感。

### Path C — DB 條件更新 0 列 → 補償

```mermaid
sequenceDiagram
    autonumber
    participant MQ as RabbitMQ
    participant INV as Inventory Service
    participant DB as Inventory DB
    participant R as Redis

    MQ->>INV: consume inventory-commit (drawRecordId=42)
    INV->>DB: UPDATE inventory SET stock = stock - 1 WHERE id = ? AND stock > 0
    Note over DB: 影響 0 列 (Redis 期間誤判有貨)
    DB-->>INV: rowcount = 0
    INV->>INV: 判定為異常扣減
    INV->>DB: UPDATE reservations SET status='REVERSED'<br/>(drawRecordId=42)<br/>不跨 DB 回寫 campaign.draw_records
    INV->>R: 校對並修正 stock:{prizeId} 計數器
    INV->>INV: 發送 alert / 對帳任務撈取
    Note over INV: 用戶端看到的「中獎」結果在此被撤銷<br/>(需在產品面上定義補償規則：如補償券)
```

- **成因**：Redis 與 DB 之間的 eventual consistency 時間窗（見 risk-control.md §5），或 Redis 資料遺失後重建期間。
- **處理原則**：**DB 為真相**——DB 說沒有就是沒有；回滾中獎結果 + 修正 Redis + alert。

### Path D — 其他失敗（活動結束 / 次數用罄 / Token 無效）

| 情境 | 偵測點 | 回應 | 說明 |
|------|--------|------|------|
| Token 過期 / 簽章無效 | Gateway | `401 Unauthorized` | ADR-009 |
| 非 ADMIN 呼叫 admin API | Gateway / Service | `403 Forbidden` | ADR-009 |
| 缺少 Idempotency-Key | Gateway | `400 Bad Request` | ADR-005 |
| 活動不存在 / 未啟動 / 已結束 | Campaign | `404` / `409` | 活動狀態檢查 |
| 活動期間抽獎次數已達上限 | Campaign | `429 Too Many Requests` | `draw_count` counter |
| RabbitMQ 暫時不可用 | Campaign publish | 重試 / dead-letter | inventory-commit 最終仍會送出（broker 恢復後） |

## 3. 時序與一致性承諾 (Timing & Consistency Semantics)

| 面向 | 承諾 |
|------|------|
| Client 收到中獎結果的時間 | 僅依賴 Redis 預扣 + Campaign DB 落庫（快速路徑），**不等待** Inventory DB 條件更新 |
| Inventory DB 最終狀態 | 透過 inventory-commit 異步達成，**eventual consistency** |
| 重複請求結果 | 與第一次完全一致（replay），**絕不重抽** |
| 實體獎品出貨數 | **絕不超過 DB 庫存**（條件更新保證），Redis 預扣只是加速層 |
