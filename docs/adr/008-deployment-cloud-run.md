# ADR-008: 部署架構 — GCP Cloud Run (Deployment on Cloud Run)

**Date:** 2026-08-13
**Status:** Accepted

## Context

本專案定位為「地端開發（POC）＋ prod 具備彈性」。部署需要回答：

1. **Prod 的運算平台**：每個 service 需要可獨立縮放（scaling）的容器化運算平台；POC 規模不適合自建 Kubernetes 叢集（運維成本高）。
2. **受管資料服務**：PostgreSQL、Redis 需要 prod 等級的 HA 與備份，不希望自建維運。
3. **消息佇列**：ADR-007 決定了 Spring Cloud Stream binder 抽象，prod 需要 RabbitMQ 的受管/可維運方案。
4. **地端一致性**：開發者必須能在筆電上用 docker-compose 複製出接近 prod 的環境。

## Decision

採用 **GCP Cloud Run 作為 service 的運算平台**，搭配 GCP 受管資料服務：

### 1. Prod 拓撲 (Topology)

```text
Internet
   │
   ▼
[Cloud Run] api-gateway ────▶ [Cloud Run] auth-service ────▶ [Cloud SQL] Auth DB
   │                          [Cloud Run] campaign-service ─▶ [Cloud SQL] Campaign DB
   └───── route               [Cloud Run] inventory-service ─▶ [Cloud SQL] Inventory DB
        │                                │
        ▼                                ▼
   [Memorystore for Redis]        [Managed RabbitMQ / VM]
```

| 元件 | Prod 方案 | 說明 |
|------|-----------|------|
| API Gateway / Auth / Campaign / Inventory | **GCP Cloud Run** | 每 service 一個 Cloud Run service，container image 由 CI build（Gradle bootJar + Jib/Dockerfile）推到 Artifact Registry |
| PostgreSQL | **GCP Cloud SQL (PostgreSQL)** | 每服務自有 schema（auth / campaign / inventory，ADR-002），可單一或分拆 instance；啟用 HA（主從 + 自動 failover）與自動備份 |
| Redis | **GCP Memorystore for Redis** | 使用 Redisson 的 Redlock 需要 Redis 2.8+（Memorystore 完全支援 Lua script）；啟用 HA tier 與 failover |
| RabbitMQ | **Managed RabbitMQ（如 CloudAMQP）或 GCP Marketplace VM** | POC 可先用 Marketplace 單機 VM 跑 RabbitMQ；流量成長後再換受管服務或切 Kafka binder（config-only，見 ADR-007） |

### 2. 地端開發環境：docker-compose

- 統一入口：**`docker/docker-compose.yml`**（README 已預告此路徑）。
- 一次啟動所有基礎設施，與 prod 元件一一對應：

```yaml
services:
  postgres:    # image: postgres:16-alpine
  redis:       # image: redis:7-alpine
  rabbitmq:    # image: rabbitmq:3-management
```

- 各 service 的 `application-dev.yml` 指向 `localhost` 上的這些 container；需要 PostgreSQL 語意驗證的開發者使用 docker 的 PostgreSQL，只需 SQLite 的開發者可切 `sqlite` profile（見 ADR-002）。

### 3. 設定與 Secrets

- **`.env.example`** 放在 repo root，列出所有環境變數（DB URL、Redis host、JWT public key、RabbitMQ 連線等），開發者複製為 `.env` 後填值。
- prod 設定透過 **Cloud Run 的環境變數 + Secret Manager** 注入：
  - 非敏感值（DB 名稱、profile）→ Cloud Run env var；
  - 敏感值（DB password、JWT private key、RabbitMQ 密碼）→ **GCP Secret Manager**，Cloud Run 以 secret ref 掛載。
- **JWT private key 只存在 auth-service 的 Secret Manager**；public key 可透過公開 endpoint 供 Gateway / 其他 service 取得（見 ADR-009）。

### 4. 地端與 prod 的設定切換

- Spring Profiles：`dev`（地端）/ `prod`（GCP）。
- `application.yml` 只放共用設定；各 profile 各自提供 DB / Redis / broker 連線。
- 雲端部署由 `gcloud run deploy` 或 CI（Cloud Build / GitHub Actions）完成，image tag 為 commit SHA 以支援 rollback。

## Consequences

**正面：**

- **零 K8s 運維**：Cloud Run 自動處理 scaling（含縮到 0）、健康檢查、滾動更新，POC 階段省下大量維運成本。
- **與地端環境高度一致**：docker-compose 元件對應 prod 元件（PostgreSQL / Redis / RabbitMQ），開發與 prod 之間的「地獄鴻溝」最小化。
- **獨立縮放**：抽獎高峰只需放大 campaign-service / inventory-service 的 Cloud Run instance 數與 Memorystore 容量。
- **受管資料層**：Cloud SQL / Memorystore 的 HA、備份、監控由 GCP 負責，符合「不自建資料庫維運」的原則。

**負面 / 需付出的代價：**

- **Cloud Run 的限制**：單一 request 有執行時間上限與 memory 上限（雖足以應付本系統）；WebSocket / 長連線場景需要額外設計（本系統不需）。
- **冷啟動（cold start）**：縮到 0 後的第一個請求有延遲。抽獎活動需維持 min-instances > 0，避免活動開場瞬間的冷啟動毛刺。
- **與 VM 相比的彈性落差**：Cloud Run 對網路、檔案系統（ephemeral，無法持久化本機磁碟）有限制；本系統是 stateless 設計，符合限制。
- **RabbitMQ 在 GCP 沒有原生受管服務**：需要第三方受管或自建 VM，是 prod 中「最不 GCP 原生」的元件；若維運成本過高，可切換 binder（見 ADR-007）。

## Alternatives

- **GKE（Google Kubernetes Engine）**：控制力與可移植性最強，但叢集維運（node pool、升級、監控）成本對 POC 過高；Cloud Run 已涵蓋本系統所需 95% 的能力，否決（保留為規模成長後的遷移目標）。
- **GCE VM + Docker Compose 直接跑**：最簡單但無自動縮放、無受管更新，高可用需自己建 load balancer + 監控，否決。
- **App Engine**：部署簡單，但 request 限制與縮放模型較不適合此類高併發短請求系統，且與 container 工作流程（docker image 共用）不如 Cloud Run 貼合，否決。
- **全 Serverless（Cloud Functions）**：執行時間限制、冷啟動與資源上限對抽獎服務不合適，否決。
