# inventory-service — SA 業務需求 (Business Requirements)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SA — Business Requirements（業務行為與語意） |
| **服務範圍** | inventory-service（庫存原子預扣、DB 條件更新真相來源、冪等、補償與對帳） |

### 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [requirements.md](../requirements.md) | 主需求清單，本文件逐一對齊其 `FR-INV-*`、`FR-X-01` 與相關 `NFR` |
| [AGENTS.md](../../../AGENTS.md) | 開發流程指引，本文件遵循其 §3 SA 層模板 |
| [campaign-service/README.md](../campaign-service/README.md) | 上游協作者之 SA 文件（預扣調用方、`inventory-commit` 發布方、降級決策方） |
| [ADR-003](../../adr/003-redis-concurrency.md) | Redis 併發控制（Lua 原子原語、key schema、Redlock） |
| [ADR-005](../../adr/005-anti-double-draw-idempotency.md) | 防重複抽獎與冪等（`drawRecordId` 作為事件冪等鍵之來源） |
| [ADR-006](../../adr/006-anti-overselling.md) | 防超抽（兩段式：Redis 預扣 + DB 條件更新 + 補償） |
| [ADR-007](../../adr/007-async-kafka-spring-cloud-stream.md) | 異步消息（`inventory-commit` 事件、at-least-once、consumer 冪等） |
| [risk-control.md](../../architecture/risk-control.md) | Redis key schema、Lua、最終一致性模型（§5）與收斂機制 |
| [draw-flow.md](../../architecture/draw-flow.md) | 抽獎生命週期與失敗路徑（Path C 補償的執行細節來源） |

> **層級界線**：本文件只定義**業務行為與語意**（use case、business rule、business state、acceptance intent、business data dictionary）。Redis key 實作、Lua script 內容、`UPDATE` SQL 語法、event payload 結構、consumer binding、DB schema/型別/index 屬 **SD 層**，不在本文件範圍；本文件引用之 ADR 中的技術細節僅作為業務語意之佐證，不在此重複設計。

---

## 2. 系統／服務定位

### 2.1 Problem & Goal

抽獎的熱門獎品會在活動開放的瞬間被大量併發請求擊中。核心不變量是：**無論多少併發，實際發放的實體獎品數絕不能超過庫存**（`FR-INV-02`）。但純 DB 條件更新正確卻受熱點鎖瓶頸所困（throughput 低），純 Redis 扣減快卻無法作為唯一真相（資料可遺失）。

inventory-service 是「**庫存真相來源 (source of truth)**」的承載者：它以**兩段式**架構同時滿足「低延遲」與「不超抽」——Redis 是**加速層**（熱點原子預扣、併發判定「還有沒有貨」），DB 是**真相層**（出貨扣減、補償判定）。本服務回答「**系統要提供什麼庫存行為**」，並以業務語意約束「結果如何才算符合目的」。

> **定位關鍵**：本服務是**純後端協作服務**，無使用者直接互動介面。它不決定抽獎結果、不決定降級，只決定「庫存扣減的真相」——誰能扣、扣多少、扣失敗了怎麼辦、如何收斂回真相。

### 2.2 Actors & User Roles

inventory-service **無 end-user（USER/ADMIN）actor**；其互動對象皆為系統內協作者：

| Role | 說明 | 主要互動 |
|------|------|----------|
| **campaign-service**（系統協作者） | 抽獎路徑上游 | 呼叫熱點庫存預扣、取得預扣結果；發布 `inventory-commit` 事件 |
| **消息佇列 (Message Broker)** | 事件傳遞介質 | 投遞 `inventory-commit`（at-least-once，可能重複投遞） |
| **對帳 job（排程）** | 系統內部排程觸發者 | 觸發定期對帳（校正 Redis、回收超時預留） |
| **OPS（營運人員）** | 異常的被動接收者 | 接收補償 alert（`FR-INV-03`、`NFR-06`），不直接操作本服務 |

> 權限語意：inventory-service **不信任任何 client**，亦無 client 可直接呼叫之狀態變更介面。一切庫存變更只能由「上游事件」或「內部排程」觸發。campaign-service 的呼叫身分由架構約定（內部服務信任），非使用者 RBAC。

### 2.3 Business Capabilities（本服務提供的能力）

1. 消費 `inventory-commit` 並執行 **DB 條件更新**（真相扣減）
2. **冪等去重**（依 `drawRecordId`）
3. **補償**（DB 條件更新影響 0 列時：回滾 + 修正 Redis counter + alert）
4. **定期對帳**（以 DB 校正 Redis、回收超時預留）— Should

> 註：抽獎路徑的「熱點庫存預扣（Redis Lua）」由 **campaign-service** 於抽獎時執行（`FR-CAMP-18`），本服務不承擔預扣決策；本服務只在其後以 `inventory-commit` 事件把扣減寫入 DB 真相。

---

## 3. Use Cases

> 每個 use case 依 AGENTS.md §3 格式：Use case name / Actor / Precondition / Main flow（編號步驟）/ Business rule / Acceptance intent，並帶 **Traceability** 行標註其實現的 `FR-*`。

---

### UC-1 消費 inventory-commit 並 DB 條件更新

- **Actor:** inventory-service 的 async consumer（消息來源：campaign-service 發布的 `inventory-commit`）
- **Precondition:** campaign-service 已抽中獎品、完成 Redis 預扣並發布 `inventory-commit`（含 `drawRecordId`、`prizeId`、`quantity`）；事件已投遞至消息佇列。
- **Main flow:**
  1. consumer 消費 `inventory-commit` 事件。
  2. 以 `drawRecordId` 做冪等檢查：若已處理過 → 直接確認（ack），不重複扣減；若未處理 → 繼續。
  3. 以**原子條件更新**寫回 DB 真相：扣減該獎品庫存，但條件為「剩餘庫存 `> 0`」。
  4. 影響 1 列 → 扣減成功，將該 reservation 標記為 `COMMITTED`，確認消息。
  5. 影響 0 列 → 進入補償流程（UC-2），不落入正常扣減，並依錯誤流程處理（`FR-X-01`）。
- **Business rule:**
  - **DB 是庫存唯一真相來源**；Redis 預扣（campaign-service 執行）只是加速層，可隨時由 DB 重建。任何時刻問「到底剩幾件」，以 DB 為準。
  - DB 條件更新是「**絕不超抽**」的最終保證：即使 Redis 誤判有貨，DB 的 `stock > 0` 條件也讓扣減失敗而非扣成負數。
  - 扣減語意：每筆 commit 扣減 `quantity` 件（本系統中每次抽獎 `quantity = 1`）。
  - consumer **冪等**：相同 `drawRecordId` 只扣減一次；消息重複投遞（at-least-once）不造成重複扣減。
  - 消費為**非同步、最終一致**：client 收到中獎結果時，DB 庫存可能尚未扣完（見 §4.2）。
- **Acceptance intent:**
  - 每筆合法 commit 使 DB 庫存準確扣減 `quantity`。
  - 相同 `drawRecordId` 重複投遞不重複扣減。
  - 影響 0 列時絕不扣成負數，且觸發補償（UC-2）。
- **Traceability:** `FR-INV-01`, `FR-INV-02`, `FR-INV-04`, `FR-X-01`

---

### UC-2 補償（DB 條件更新影響 0 列）

- **Actor:** inventory-service（補償邏輯，由 UC-1 的「0 列」結果觸發）
- **Precondition:** UC-1 的 DB 條件更新回報影響 0 列（Redis 與 DB 帳面不一致，或 Redis 資料遺失重建期間的誤判）。
- **Main flow:**
  1. 偵測到條件更新影響 0 列 → 判定為**異常扣減**。
  2. 記錄異常並**回滾該次中獎結果**：將對應抽獎記錄標記 `VOID`／產生 `draw_reversal`，撤銷其「中獎」語意。
  3. **修正 Redis counter**：將誤扣的預扣額度加回（以 DB 真相為準）。
  4. **發出 alert** 通知營運（OPS）。
  5. 後續以 DB 為準收斂 Redis（由對帳 job，UC-3）。
- **Business rule:**
  - **DB 為真相**：DB 說沒有就是沒有，不得因 Redis 誤判而超發。
  - 補償使「使用者看到的『中獎』結果被撤銷」——此為已接受的業務代價（詳見 §4.4 語意）。補償的產品規則（如發補償券）**超出本服務職責**；本服務只負責「回滾 + 修正 Redis + alert」。
  - 補償後 Redis counter 必須向 DB 真相收斂：Redis 只能被「加回」修正，**不得因此超於 DB**。
  - alert 是異常的可觀察性出口（`NFR-06`）。
- **Acceptance intent:**
  - 0 列更新時不產生負庫存；該筆中獎被撤銷、Redis 額度被加回、發出 alert。
  - 補償後系統持續收斂至 DB 真相。
- **Traceability:** `FR-INV-03`, `FR-X-01`

---

### UC-3 定期對帳 (Reconciliation Job)

- **Actor:** 排程對帳 job（系統內部，**Should** 優先級）
- **Precondition:** 活動進行中（或結束後清算）；Redis 與 DB 之間存在不一致窗口，或存在遺留未收尾之預留。
- **Main flow:**
  1. 以 **DB 剩餘庫存為基準**，校正 Redis counter（將 Redis 對齊 DB 真相）。
  2. 掃描 reservations 中**超過預留 TTL 仍未 commit** 的記錄 → 回收其 Redis 預扣額度（加回）。
  3. 記錄對帳結果供稽核（audit log，屬可觀察性，非營運報表）。
- **Business rule:**
  - 對帳是**收斂機制**（eventual consistency 的收斂手段），**不改寫 DB 真相**，只校正 Redis 加速層與回收超時預留。
  - 校正方向**單一**：Redis 對齊 DB，而非 DB 遷就 Redis。
  - 屬 **Should** 優先級（prod 前完成），非 Must。
- **Acceptance intent:**
  - 對帳後 Redis counter 與 DB 庫存一致（在正常收斂語意下）。
  - 超時未 commit 的預留被回收，Redis 額度不再被永久佔用。
- **Traceability:** `FR-INV-05`

---

## 4. Business State

### 4.1 庫存狀態模型 (Inventory State Model)

庫存狀態由**兩個層**構成，各自角色明確：

| 層 | 狀態 | 角色 | 一致性角色 |
|----|------|------|-----------|
| Redis counter `stock:{prizeId}` | 加速層鏡像（剩餘可預扣額度） | 低延遲預扣、併發判定「還有沒有貨」 | 可暫時不精確、可重建 |
| DB `inventory.stock` | 真相來源（剩餘可出貨數量） | 出貨扣減、補償判定 | 必須精確、不可超扣 |

> **語意**：兩者都是「剩餘庫存」的表示，但**DB 為唯一真相**。Redis 是 DB 的近似鏡像，僅用於熱點路徑加速；它「少」可接受（可能把仍有貨誤判為沒貨，導致降級），「多」不允許（不可誤判有貨而超發）——這方向性由預扣先行 + DB 條件更新兜底共同保證。

### 4.2 不一致窗口語意 (Consistency Window Semantics)

```text
T0  campaign 執行 Redis 預扣 stock 50→49   (加速層，campaign-service 執行)
T1  campaign 落庫 draw_record               (上游)
T2  campaign 發布 inventory-commit ──────►  inventory-service
T3  inventory 條件更新 DB stock 50→49       (真相層)
                  │
                  └── T0~T3 之間：Redis=49、DB=50（不一致窗口）
```

- **不一致窗口大小** ≈ 消息延遲 + consumer 處理時間（通常 ms ~ 秒級）。
- **方向性**：Redis 只可能「比 DB 少」（預扣先行），**不可能「比 DB 多」**——除非發生補償回滾（UC-2 Path C）。此方向性保證「DB 真相」永不超扣。
- **可接受的理由**：抽獎是「短暫高併發、後台寫回」的場景，client 不需要即時看到「剩 N 件」的精確值；唯一不可妥協的是「實發 ≤ DB 庫存」，由 DB 條件更新保證，與 Redis 即時性無關。

### 4.3 預留生命週期 (Reservation Lifecycle)

```text
RESERVED ──► COMMITTED  (終態)
     │
     └────► REVERSED   (終態；0 列補償 or 超時回收)
```

| 狀態 | 業務意義 | 進入條件 |
|------|----------|----------|
| `RESERVED` | 預扣已發生（campaign-service 已於 Redis 預扣），等待 `inventory-commit` | campaign-service 抽獎命中並預扣成功 |
| `COMMITTED` | commit 已消費，DB 條件更新成功，DB 已扣 | UC-1 影響 1 列 |
| `REVERSED` | 補償回滾（0 列更新）或超時回收，Redis 額度加回 | UC-2（0 列）或 UC-3（超時回收） |

### 4.4 業務語意問題（AGENTS.md §3 要求回答）

1. **狀態能否回轉？**
   - DB 庫存扣減為**單向**（只扣不加），正常業務流程中不可加回；唯一「加回」是補償修正（`REVERSED`），屬異常回滾而非正常回轉。
   - Reservation 狀態機：`RESERVED → COMMITTED` 單向；`RESERVED → REVERSED` 單向（補償／超時）。`COMMITTED` 與 `REVERSED` 皆為終態，不可回轉、不可再 commit。

2. **使用者看到的是本系統狀態還是 upstream 狀態？**
   - inventory-service 的 **DB 庫存是下游真相**；campaign-service 抽獎路徑看到的 Redis 預扣額度，是「庫存加速層」的**可暫不精確**狀態（由 campaign-service 於抽獎時執行預扣維護）。
   - end-user 不直接讀取庫存精確餘額——庫存精確餘額不即時暴露；使用者只感知「中獎／銘謝惠顧」的結果，其背後庫存由本服務與 campaign-service 協作決定。

3. **哪些角色能改狀態？**
   - **無任何使用者角色可直接改庫存**。DB 庫存扣減由 `inventory-commit` 事件觸發（UC-1）；Redis counter 由 campaign-service 預扣、本服務補償修正（UC-2）／對帳校正（UC-3）驅動；補償由異常路徑觸發。
   - OPS 僅被動接收 alert，**不能直接改寫**庫存或預留狀態。

4. **哪些欄位不能被使用者直接改？**
   - 庫存剩餘數（`stock`）：不得由 client／使用者直接設定，只能經**條件扣減**（UC-1）。
   - Redis counter：只能由 campaign-service 預扣／本服務補償／對帳路徑修改，無對外寫入介面。
   - `drawRecordId` 冪等鍵：由上游 `draw_record` 決定（event 攜帶），consumer **不得自行生成**。

---

## 5. Business Data Dictionary

> 本表定義**欄位的業務意義**（SA 層）。型別、DB type、constraint、index、Redis key 形式屬 SD 之 Technical Data Dictionary，**不在本表定義**。

### 5.1 inventory（庫存）

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `prize_id` | 對應之獎品識別（與 campaign-service 獎品同一識別） | 是 | 有效獎品識別 | campaign-service 獎品配置（ADMIN 建立） | internal |
| `stock` / `remaining` | 剩餘可發放數量（真相來源） | 是 | 非負整數；條件更新保證 ≥ 0 | 初始值來自 ADMIN 獎品配置（`FR-CAMP-02`）；後續由 inventory-service 條件扣減 | sensitive（營運） |

> **語意註記**：庫存真相的**初始值**來自 campaign-service 的獎品配置（`FR-CAMP-02` 的 `quantity`），inventory-service 以其為初始真相；此後一切扣減與剩餘數以本服務為準。初始值如何進入本服務（同步初始化／首次預扣時建立）屬 SD 層，不在本文件設計。

### 5.2 reservations（預留記錄）

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| `draw_record_id` | 抽獎記錄識別；consumer 冪等去重鍵（每筆中獎唯一） | 是 | 唯一、對應一筆上游 `draw_record` | campaign-service 發布之 `inventory-commit` event | internal |
| `prize_id` | 預留之獎品 | 是 | 有效獎品識別 | event 攜帶 | internal |
| `quantity` | 預留件數（本系統每次抽獎 `= 1`） | 是 | 正整數（= 1） | event 攜帶 | internal |
| `status` | 預留生命週期狀態 | 是 | `RESERVED` / `COMMITTED` / `REVERSED` | inventory-service 狀態機（§4.3） | internal |
| `reserved_at`（預留時間） | 預扣發生的時間；超時回收之 TTL 基準 | 是 | 合法時間 | inventory-service（預扣時記錄） | internal |

> **語意註記**：`draw_record_id` 是 reservation 的**冪等識別**（`FR-INV-04`）——同一 `draw_record_id` 對應至多一筆 reservation、至多一次 DB 扣減；消息重複投遞撞此鍵即忽略。`reserved_at` 是「超時未 commit」判定（`FR-INV-05`）的依據。

---

## 6. Acceptance Criteria

> 每條以 GIVEN/WHEN/THEN 描述，標註 AC ID 與對應 FR。

### 6.1 DB 條件更新 (Conditional Update)

**AC-INV-001 — 條件更新影響 1 列**（`FR-INV-01`, `FR-INV-02`）
- GIVEN 消費一筆 `inventory-commit` 且 DB 剩餘庫存 `> 0`
- WHEN consumer 執行條件更新
- THEN 影響 1 列、DB 庫存扣減 `quantity`、該 reservation 標記 `COMMITTED`、消息確認

**AC-INV-002 — 條件更新影響 0 列觸發補償**（`FR-INV-03`）
- GIVEN DB 條件更新回報影響 0 列
- WHEN consumer 偵測到此結果
- THEN 執行補償：回滾中獎結果（`VOID`／`draw_reversal`）、修正 Redis counter（加回誤扣額度）、發出 alert；庫存絕不為負

### 6.2 冪等 (Idempotency)

**AC-INV-003 — consumer 冪等去重**（`FR-INV-04`）
- GIVEN 相同 `drawRecordId` 的 `inventory-commit` 被重複投遞
- WHEN consumer 再次消費
- THEN 不重複扣減 DB、僅確認消息；該 `drawRecordId` 對應的扣減僅發生一次

### 6.3 收斂與對帳 (Convergence & Reconciliation)

**AC-INV-004 — 最終一致性收斂（正常收斂）**（`FR-INV-02`, `FR-INV-05`）
- GIVEN 一批 `inventory-commit` 被消費且 DB 條件更新成功
- WHEN 對帳 job 執行（以 DB 為準校正 Redis）
- THEN Redis counter 收斂至 DB 庫存；不一致窗口內 Redis ≤ DB，無超發

**AC-INV-005 — 對帳回收超時預留**（`FR-INV-05`）
- GIVEN reservations 中存在超過預留 TTL 仍未 commit 的記錄
- WHEN 對帳 job 掃描
- THEN 回收其 Redis 預扣額度（加回），使 Redis 額度不再被永久佔用

### 6.4 錯誤流程 (Error Flow)

**AC-INV-006 — 錯誤流程語意**（`FR-X-01`）
- GIVEN consumer 遭遇訊息格式不合法／重複投遞／0 列更新等錯誤情境
- WHEN 依各自情境處理
- THEN 重複投遞 → 冪等忽略；0 列 → 補償（UC-2）；格式不合法／不可處理 → 進入錯誤處理（如 dead-letter）且無副作用；任何路徑都不造成負庫存、不重複扣減，並具可觀察性（log/alert）

---

## 7. Out of Scope（本 SA 文件不涵蓋）

| 項目 | 說明 | 後續層／歸屬 |
|------|------|--------------|
| 補貨／庫存追加 | 庫存只能扣減，不提供補貨機制（初始庫存由 campaign-service 的獎品配置提供，`FR-CAMP-02`） | campaign-service（配置） |
| 預留／釋放生命週期 API | 不提供 reserve/release 的對外 API 或生命週期端點；預留生命週期由內部流程（預扣／commit／補償／對帳）驅動 | —（刻意不做） |
| 庫存報表 | 不產出營運報表；對帳 job 僅「校正 Redis + 回收超時預留」並留 audit log，非報表 | —（刻意不做） |
| 批次出貨／發貨整合 | 不處理批次出貨、物流、發券等 | 其他服務／未來整合（requirements.md §4） |
| 對帳以外的排程 job | 除定期對帳（`FR-INV-05`）外，不新增其他排程任務 | —（刻意不做） |
| 庫存預警（低庫存提醒） | 不提供低庫存預警；alert 僅限於「0 列補償」異常（`FR-INV-03`、`NFR-06`） | —（刻意不做） |
| 降級銘謝惠顧決策 | 屬 campaign-service（`FR-CAMP-19`）；本服務只負責 DB 條件更新，不參與抽獎路徑的降級決策 | campaign-service spec |
| 補償的產品規則（如補償券） | 補償後「如何安撫使用者」超出本服務；本服務只回滾 + 修正 + alert | 產品層決策 |
| API 路由／OpenAPI schema／DB schema／Redis key／Lua 實作 | 具體 endpoint、request/response、型別、constraint、index 屬 SD（`FR-X-03`、`FR-X-04`） | SD / `docs/api/`、`docs/db/` |
| 身份驗證／授權 | inventory-service 無使用者端點，不涉及 RBAC（`FR-GW-*`、`FR-AUTH-*` 屬其他服務） | Auth/Gateway spec |

---

## 附錄 A：需求追溯矩陣 (Traceability Matrix)

| FR ID | 需求摘要 | 由 UC 實現 | 由 AC 驗證 |
|-------|----------|-----------|------------|
| FR-INV-01 | 消費 `inventory-commit` + DB 條件更新 | UC-1 | AC-INV-001 |
| FR-INV-02 | 實發獎品數絕不超過庫存（DB 條件更新為最終保證） | UC-1 | AC-INV-001, AC-INV-004 |
| FR-INV-03 | DB 條件更新影響 0 列時補償（回滾 + 修正 Redis + alert） | UC-2 | AC-INV-002 |
| FR-INV-04 | consumer 冪等（`drawRecordId` 去重） | UC-1 | AC-INV-003 |
| FR-INV-05 | 定期對帳 job（校正 Redis、回收超時預留）— Should | UC-3 | AC-INV-004, AC-INV-005 |
| FR-X-01 | 錯誤流程與輸入驗證 | UC-1, UC-2 | AC-INV-006 |
| NFR-03（引用） | 高併發一致性（加速層 + 真相層 + 補償對帳） | UC-1~UC-3（語意） | AC-INV-002, AC-INV-004 |
| NFR-06（引用） | 可觀察性（補償告警、對帳記錄） | UC-2, UC-3 | AC-INV-002, AC-INV-005, AC-INV-006 |
