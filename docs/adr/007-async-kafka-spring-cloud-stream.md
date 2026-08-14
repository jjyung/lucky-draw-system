# ADR-007: 異步消息 — Spring Cloud Stream + RabbitMQ (Asynchronous Messaging)

**Date:** 2026-08-13
**Status:** Accepted

## Context

抽獎流程需要跨 service 的非同步協作（見 ADR-006）：campaign-service 抽中實體獎品後，必須通知 inventory-service 扣庫存。同步 HTTP 呼叫的問題：

- campaign-service 必須等 inventory-service 回應才回 client → **延遲增加**、**兩 service 強耦合**（availability 互相拖累）。
- 抽獎高鋒時，同步呼叫會把請求放大打到 inventory-service。

需要一個**非同步、可重放、可水平擴展**的消息層。候選 broker：Kafka、RabbitMQ、GCP Pub/Sub。

**關鍵考量**：本專案是 POC 起步但預留 prod 彈性。POC 階段希望 broker **輕量、開發者在地端用 docker-compose 一鍵啟動**；prod 階段希望可換到 Google 管理的消息服務或 Kafka。因此 abstraction 層比具體 broker 選擇更重要。

## Decision

採用 **Spring Cloud Stream（functional + binding abstraction）+ RabbitMQ binder**：

### 1. Framework：Spring Cloud Stream

- 使用 Spring Cloud Stream 的 **functional programming model**（`Function`/`Supplier`/`Consumer` bean + `spring.cloud.stream.bindings.*` 設定）。
- Broker 的選擇被抽象為 **binder**，業務 code 只依賴 binding 名稱，不依賴 RabbitMQ/Kafka 的具體 client API。
- **未來的 prod 若要換 Kafka / Google Pub/Sub，只需換 binder dependency + 改設定檔，業務 code 零改動（config-only change）。**

### 2. Broker（POC）：RabbitMQ

- POC / 地端開發使用 **RabbitMQ binder**。
- **這是刻意（intentional）的決策**：不選 Kafka 當預設，是因為 POC 階段 RabbitMQ 的 docker-compose 啟動輕量、概念簡單、binding 開銷低；Kafka 的 partition 管理與學習成本在 POC 階段是 over-engineering。Cloud Stream abstraction 保留了 prod 換 Kafka 的彈性。

### 3. Topic / Binding 定義

| Binding | 方向 | 事件 | Payload (Key) | 生產者 | 消費者 |
|---------|------|------|----------------|--------|--------|
| `draw-result` | publish / subscribe | 抽獎結果（中獎 / 銘謝惠顧） | `DrawResultEvent` (drawRecordId) | campaign-service | 營運側/報表消費者（可選） |
| `inventory-commit` | publish / subscribe | 庫存寫回請求 | `InventoryCommitEvent` (drawRecordId, prizeId, quantity) | campaign-service | inventory-service |
| `prize-stock-configured` | publish / subscribe | 庫存初始/差值同步 | `PrizeStockConfiguredEvent` (prizeId, oldQuantity, newQuantity, configVersion) | campaign-service | inventory-service |

- Event payload 為**共用 DTO**（放在 `common` module，見 ADR-001），內容以 **`draw_record_id` 為冪等鍵**（見 ADR-005 / 006）。
- 每個 service 自己的 consumer 屬於獨立的 consumer group，避免多 instance 重複消費。

### 4. 地端環境

- RabbitMQ 在 **`docker/docker-compose.yml`** 以 container 啟動（見 ADR-008）。
- 開發環境可在 `application-dev.yml` 用 `spring.cloud.stream.bindings.<name>.destination` 指到對應 exchange/queue。

## Consequences

**正面：**

- **解耦與彈性**：campaign-service 發布 `inventory-commit` 後立刻回 client，inventory-service 可獨立縮放、獨立故障，不拖累主抽獎路徑。
- **Broker 可替換**：prod 換 Kafka / Pub/Sub 是 config-only change，符合「POC 輕量、prod 有彈性」的定位。
- **背壓與重試**：消息佇列天然提供緩衝，consumer 慢不會直接反壓 campaign-service；失敗可 retry（binder 的 retry 設定）。

**負面 / 需付出的代價：**

- **RabbitMQ 與 Kafka 語意差異**：RabbitMQ（queue-based）是 **competing consumers** 模型，Kafka 是 **partition + offset** 模型。換 binder 時**消費順序與 exactly-once 語意需要重新驗證**，並非「零思考」的切換。
- **At-least-once**：消息可能重複投遞，consumer 必須**冪等**（用 `draw_record_id` 去重），不能假設 exactly-once。
- **最終一致性**：client 拿到中獎結果時，DB 庫存可能還沒扣完（見 ADR-006），需要對帳機制收斂。
- **額外基礎設施**：本地需要跑 RabbitMQ container；prod 需要管理的 RabbitMQ（見 ADR-008）。

## Alternatives

- **同步 HTTP（RestTemplate/Feign）直接呼叫 inventory-service**：延遲高、availability 耦合、無緩衝，抽獎高峰會放大到 inventory，否決。
- **直接採用 Kafka binder（預設即 Kafka）**：Kafka 的持久化、partition 順序與 exactly-once 能力在 prod 確實更強，但 POC 階段維運與地端啟動成本高；本決策**保留 Kafka 作為 prod binder 候選**（見 ADR-008「go prod」路徑），POC 階段刻意選 RabbitMQ，未來可透過 config-only 切換。
- **GCP Pub/Sub binder**：若確定 prod 只走 GCP，Pub/Sub 是最少維運的選擇；但為了避免 POC 階段綁死 GCP 專案（地端開發無法使用），否決，保留為 prod 選項之一。
- **自寫消息抽象層（不依賴 Spring Cloud Stream）**：自行封裝 RabbitTemplate/KafkaProducer 會重造輪子且測試成本高，Spring Cloud Stream 已是 Spring Boot 生態標準，否決。
