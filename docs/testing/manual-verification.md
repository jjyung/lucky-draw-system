# 手動驗證手冊 (Manual Verification Guide)

> 對應 [`docs/stories/journeys.md`](../stories/journeys.md) 的 **J-1 / J-2 / J-3**，以 `curl` 手動走完三個旅程，
> 驗證「四個服務 + Redis + RabbitMQ」**真實協作**（E2E）。此手冊同時是未來回歸自動化（腳本）的藍本。
>
> 所有請求皆打到 **API Gateway（`http://localhost:8080`）**，路徑帶 `/api/v1` 前綴。回應統一 envelope `{ code, message, data }`。

---

## 0. 前置準備

```bash
# 1. 啟動基礎設施（Redis + RabbitMQ；Postgres 可選——服務預設用 H2 MODE=PostgreSQL）
docker compose -f docker/docker-compose.yml up -d

# 2. 建置（JDK 21）
export JAVA_HOME=~/.jdks/corretto-21.0.5   # Windows: $env:JAVA_HOME="...\corretto-21.0.5"
cd app
./gradlew build

# 3. 啟動四個服務（各開一個 terminal；建議順序 auth → campaign → inventory → gateway）
./gradlew :auth-service:bootRun       # :8081（需先起，寫 jwt:public-key 到 Redis）
./gradlew :campaign-service:bootRun   # :8082
./gradlew :inventory-service:bootRun  # :8083（無 REST，僅事件消費；啟動時帳目校對種子 Redis 庫存）
./gradlew :api-gateway:bootRun        # :8080（唯一對外入口）

# 4. 健康檢查
curl http://localhost:8080/actuator/health
```

> ⚠️ 限流預設 per-user 10 req/s、per-IP 100 req/s（`gateway.rate-limit`）；手動快速連打可能觸發 `429/A0500`，稍候即恢復。
> ⚠️ `Idempotency-Key` 每次抽獎用**新的 UUID**；replay 用**同一個**。可用 `uuidgen`（macOS/Linux）或手動貼一個。

---

## J-2 抽獎者旅程（USER）

### 1. 註冊
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"S3cure!Pass"}'
```
**期望**：`200`，`data` 含 `id`/`username`/`email`/`roles:["ROLE_USER"]`（**不含密碼**）。

### 2. 登入（記下 accessToken）
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"S3cure!Pass"}'
```
**期望**：`200`，`data.accessToken`（RS256 JWT，claims 承載 `sub`/`roles`）。**驗證點**：Redis 出現 `auth:token:{jti}`（`redis-cli KEYS 'auth:token:*'`）。

### 3. 瀏覽活動列表（PUBLIC，免登入）
```bash
curl http://localhost:8080/api/v1/campaigns
```
**期望**：`200`，含種子活動「2026 中秋轉盤抽獎」；**不暴露 `drawLimit`**。

### 4. 瀏覽活動詳情（PUBLIC）
```bash
curl http://localhost:8080/api/v1/campaigns/1
```
**期望**：`200`，含獎品清單 `id/name/type`；**不暴露 `probability`/`quantity`**。

### 5. 抽獎（單次，需 `Authorization` + `Idempotency-Key`）
```bash
curl -X POST http://localhost:8080/api/v1/campaigns/1/draw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Idempotency-Key: <UUID-A>" \
  -d '{"count":1}'
```
**期望**：`200`，`data` 為單一結果：`{ drawRecordId, campaignId, resultType: WIN|THANK_YOU, prize }`（THANK_YOU 時 `prize: null`）。
**驗證點**：抽中 `WIN` 後，`redis-cli GET stock:1`（iPhone 剩餘 -1）、inventory-service log 出現「扣減」日誌（非同步，ms~s 內）。

---

## J-3 重送與防超抽旅程

### 6. replay：用「同一個」Idempotency-Key 重送第 5 步
```bash
# 同上，但 Idempotency-Key 仍用 <UUID-A>
```
**期望**：`200`，body 與第 5 步**逐位元一致**（同 drawRecordId，不重抽、不重扣、不重計）。

### 7. 抽獎次數超限（seed 活動 draw_limit=10）
連抽超過 10 次（不同 key）→ **期望**：`429`，`code: "A0306"`（個人抽獎次數已達上限）。

---

## J-1 營運旅程（ADMIN）

### 8. 登入 admin（seed：`admin` / `admin123`）
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
**期望**：`200`，取得 `accessToken`（claims `roles` 含 `ROLE_ADMIN`）。

### 9. 建立活動（ADMIN；建立後為 DRAFT）
```bash
curl -X POST http://localhost:8080/api/v1/campaigns \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"name":"手動驗證活動","startTime":"2026-09-01T00:00:00Z","endTime":"2026-10-01T00:00:00Z","drawLimit":5}'
```
**期望**：`201`，`data.id`（記下 `<ID>`，`status: "DRAFT"`）。

### 10. 配置獎品（總和 = 100%）
```bash
curl -X PUT http://localhost:8080/api/v1/campaigns/<ID>/prizes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"prizes":[{"name":"iPhone","type":"PRIZE","probability":5,"quantity":1},{"name":"銘謝惠顧","type":"THANK_YOU","probability":95,"quantity":0}]}'
```
**期望**：`200`，回傳含系統產生的獎品 `id`。**驗證點**：inventory-service 收到 `prize-stock-configured`（log 可見），inventory 表新增一列（prize stock=1）。

### 11. 啟用活動
```bash
curl -X PATCH http://localhost:8080/api/v1/campaigns/<ID>/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"status":"ACTIVE"}'
```
**期望**：`200`，`status: "ACTIVE"`。此後 USER 可對 `<ID>` 抽獎。

---

## 負向驗證（權限與錯誤流程，FR-X-01）

| 情境 | 指令重點 | 期望 |
|------|----------|------|
| 無 token 抽獎 | 第 5 步去掉 `Authorization` | `401` / `A0203` |
| USER token 建活動 | 第 9 步用 `<USER_TOKEN>` | `403` / `A0400` |
| 缺 Idempotency-Key 抽獎 | 第 5 步去掉該 header | `400` / `A0501` |
| 登出後再抽 | `POST /api/v1/auth/logout`（帶 token）後，再用同 token 抽獎 | `401` / `A0203`（已撤銷） |
| 機率總和 ≠ 100% | 第 10 步 probability 改成總和 90 | `422` / `A0303` |

---

## 完成標準（Definition of Done）

- [ ] 四個服務 health 全綠；gateway 路由正常。
- [ ] J-2：註冊→登入→瀏覽→抽獎，`WIN`/`THANK_YOU` 結果正確。
- [ ] J-3：replay 逐位元一致；次數超限 `429/A0306`。
- [ ] J-1：admin 建活動→配獎品→啟用，inventory 同步收到 `prize-stock-configured`。
- [ ] 負向：`401`/`403`/`400`/`A0306`/`A0303` 各回正確碼；登出後 token 失效。
- [ ] Redis：`stock:{prizeId}` 隨中獎遞減、`auth:token:{jti}` 登入存在／登出消失。
