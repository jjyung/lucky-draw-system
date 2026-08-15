# API 規格總覽 (API Specification Overview)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SD — API Contract（邊界與介面、安全與錯誤、NFR 契約、API ID 清單、追溯矩陣） |
| **範圍** | 4 服務對外/事件介面的統一規約 + 逐服務 OpenAPI 3.0 規格 |

### 關聯文件 (Related Documents)

| 文件 | 說明 |
|------|------|
| [requirements](../specs/requirements.md) | FR/NFR，本文件對齊其 `FR-X-01/03/04`、各服務 FR |
| [各服務 SA](../specs/) | 業務語意與 Acceptance Criteria |
| [ADR 索引](../adr/README.md) | 架構決策（尤其 ADR-005/006/007/009/010） |
| [資料庫設計](../db/README.md) | DB schema（本文件的 field 對照來源） |
| [《RESTful API 設計指南》](../rules/RESTful%20API%20設計指南（工程實務筆記）.md) | REST 命名/方法/status code/envelope 規約 |
| [《OpenAPI Contract Rules》](../rules/OpenAPI%20Contract%20Rules.md) | API ID / operationId / Model 命名規約 |
| [《錯誤碼》](../rules/錯誤碼.md) | 數字錯誤碼分段規約 |

---

## 2. 統一規約 (Global Conventions)

### 2.1 Base Path 與版本

- 所有對外端點前綴 **`/api/v1`**（`/api` + `/v{n}` 兩段，ADR-009）。內部服務間呼叫/事件**不走**此前綴。
- 資源路徑採 **resource-oriented**：複數名詞 + `{resourceId}`；非 CRUD 業務動作採 `POST /resource/{id}/{action}`（見《RESTful API 設計指南》§十二.2 例外）。

### 2.2 回應 Envelope（成功與失敗統一）

所有回應（含錯誤）一律使用相同 envelope：

```json
{
  "code": "00000",
  "message": "OK",
  "data": {}
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `code` | string | 數字錯誤碼（成功 `00000`；錯誤見 §2.3） |
| `message` | string | 人類可讀訊息；成功時亦可補充說明 |
| `data` | object / array / null | 成功承載資源；失敗為 `null` |

- **抽獎 replay（ADR-005）**：同一複合冪等鍵重送時，回傳的 `code`/`message`/`data` 與首次**逐位元一致**（皆 `200 OK`），client 無法以 status 區分首次與重放。

### 2.3 錯誤碼與 HTTP Status 對映

`code` 採數字分段（《錯誤碼》）：`Axxxx` 用戶端錯誤、`Bxxxx` 系統錯誤、`Cxxxx` 第三方錯誤。

| code | HTTP | 語意 | 來源服務 |
|------|------|------|----------|
| `00000` | 200 | 成功 | 全部 |
| `A0000` | 400 | 用戶端錯誤（一級） | 全部 |
| `A0100` | 400/409 | 註冊錯誤（二級） | auth |
| `A0101` | 409 | username 已存在 | auth |
| `A0102` | 409 | email 已存在 | auth |
| `A0103` | 400 | 註冊輸入驗證失敗 | auth |
| `A0200` | 401 | 登入異常（二級） | auth |
| `A0201` | 401 | 帳號或密碼錯誤（與帳號不存在同碼，不洩漏存在性） | auth |
| `A0202` | 401 | 憑證過期 | auth / gateway |
| `A0203` | 401 | 憑證無效（簽章/格式） | auth / gateway |
| `A0300` | 4xx | campaign 錯誤（二級） | campaign |
| `A0301` | 404 | 活動不存在（或非 ACTIVE 之抽獎請求） | campaign |
| `A0302` | 409 | 活動狀態衝突（非法轉移 / 非可編輯狀態） | campaign |
| `A0303` | 400/422 | 獎品機率總和 ≠ 100% | campaign |
| `A0304` | 400/422 | 獎品機率越界 `[0,100]` | campaign |
| `A0305` | 400/422 | 缺 `THANK_YOU` 獎品 | campaign |
| `A0306` | 429 | 個人抽獎次數超限（活動期間總額） | campaign |
| `A0307` | 409 | 冪等鍵衝突（併發重入，見 ADR-005） | campaign |
| `A0400` | 403 | 權限不足（越權存取管理功能） | 各服務 |
| `A0500` | 429 | 請求過於頻繁（gateway 限流，FR-GW-03） | gateway |
| `A0501` | 400 | 抽獎請求缺 Idempotency-Key（FR-GW-04） | gateway |
| `B0000` | 500 | 系統錯誤（一級） | 全部 |
| `C0000` | 502/504 | 呼叫第三方/下游錯誤 | 全部 |

> 各服務 YAML 可在其 `components` 內補充更細的 code，但**必須沿用上表已定義的 code 與 status 對映**，不得重複定義語意。

### 2.4 身份驗證與授權

- **auth scheme**：JWT **RS256** bearer token（ADR-009），`Authorization: Bearer <token>`。
- **公鑰公開**：`GET /api/v1/auth/.well-known/jwks.json`（JWKS，支援多 key 輪替）。
- **Gateway 傳遞**（僅內部）：`X-User-Id`、`X-User-Roles`；原始 `Authorization` 不下游透傳（ADR-009）。
- **Defense in depth**：各服務獨立以 public key 複驗 JWT，不信任傳遞 header（ADR-009 §3）。
- **授權**：以 JWT `roles` claim 為準；`ROLE_ADMIN` 才可存取管理端點，越權回 `403`（`A0400`）。角色判定不信任 client 傳入欄位。

### 2.5 Idempotency-Key（抽獎路徑）

- `POST /api/v1/campaigns/{campaignId}/draw` 必須帶 **`Idempotency-Key: <UUID>`** header；gateway 缺此 header 回 `400`（ADR-005、FR-GW-04）。
- 複合冪等鍵 = `userId + campaignId + idempotencyKey`；同鍵重送回傳原結果（replay，§2.2）。

### 2.6 限流 NFR（ADR-003 / FR-GW-03）

| 維度 | 預設門檻 | 說明 |
|------|----------|------|
| per-user | 10 req/s | 依已驗證身份（`userId`） |
| per-IP | 100 req/s | 依來源位址 |

- 門檻與窗口**由環境變數控制**（`NFR-04`）：如 `RATE_LIMIT_USER_RPS`、`RATE_LIMIT_IP_RPS`。
- 超限回 `429`，不轉發下游（FR-GW-03）。此與業務層「抽獎次數超限 `A0306`」為不同維度。

### 2.7 命名規約（詳見《OpenAPI Contract Rules》）

- **API ID**：`{service-name}-{resource-name-plural}-{3-digit-seq}`，於每個 operation 的 `summary`/`description` 標註，為 §3 API 清單的索引鍵。
- **operationId**：camelCase `{verb}{Resource}{action?}`，如 `getCampaigns`、`postCampaignDraw`。
- **Model**：`{HttpMethod}{Resource}RequestDTO/ResponseDTO`，`Resource` 單複數依 API 語意。

### 2.8 分頁（POC 範圍）

- `GET /api/v1/campaigns`（活動列表）POC 階段**不分頁**（活動數小），直接回傳全量。大量資料時預留 cursor 演化（AGENTS.md §4）。

---

## 3. API ID 清單 (API Index — Source of Truth)

> 此為 **OpenAPI YAML 的索引**（《OpenAPI Contract Rules》）：每個 operation 的 API ID / operationId 必須與本清單一致。

| API ID | Method | Path | 角色 | 摘要 | operationId |
|--------|--------|------|------|------|-------------|
| `auth-users-001` | POST | `/api/v1/auth/register` | PUBLIC | 使用者註冊 | `postAuthRegister` |
| `auth-tokens-001` | POST | `/api/v1/auth/login` | PUBLIC | 登入並簽發憑證 | `postAuthLogin` |
| `auth-tokens-002` | POST | `/api/v1/auth/refresh` | USER（Should） | 刷新存取憑證 | `postAuthRefresh` |
| `auth-keys-001` | GET | `/api/v1/auth/.well-known/jwks.json` | PUBLIC | 取得驗證公鑰（JWKS） | `getAuthJwks` |
| `campaign-campaigns-001` | GET | `/api/v1/campaigns` | PUBLIC | 活動列表（不含管理欄位） | `getCampaigns` |
| `campaign-campaigns-002` | GET | `/api/v1/campaigns/{campaignId}` | PUBLIC | 活動詳情 | `getCampaignById` |
| `campaign-campaigns-003` | POST | `/api/v1/campaigns` | ADMIN | 建立活動 | `postCampaigns` |
| `campaign-campaigns-004` | PUT | `/api/v1/campaigns/{campaignId}` | ADMIN | 更新活動（全量） | `putCampaignById` |
| `campaign-campaigns-005` | PATCH | `/api/v1/campaigns/{campaignId}/status` | ADMIN | 活動狀態轉移 | `patchCampaignStatus` |
| `campaign-prizes-001` | PUT | `/api/v1/campaigns/{campaignId}/prizes` | ADMIN | 配置獎品與機率（整批） | `putCampaignPrizes` |
| `campaign-draws-001` | POST | `/api/v1/campaigns/{campaignId}/draw` | USER | 抽獎（單次/批次） | `postCampaignDraw` |

**inventory-service**：無對外 REST 端點；其契約為**事件**（§4.3），不列入本 REST 清單。
**gateway-service**：無自有業務端點；其規格為**對外 surface 彙整**（§4.4）。

---

## 4. 規格檔案索引 (Spec Files)

```
docs/api/
├── README.md                      # 本文件：統一規約 + API 清單 + 追溯矩陣
└── openapi/
    ├── auth-service.yaml          # auth-service REST（§3 的 auth-*）
    ├── campaign-service.yaml      # campaign-service REST（§3 的 campaign-*）
    ├── inventory-service.yaml     # inventory-service 事件契約（無 REST paths，僅 message schemas）
    └── gateway-service.yaml       # 對外 surface 彙整（安全方案/限流/冪等 header + 路由）
```

### 4.1 auth-service.yaml 涵蓋
`auth-users-001`、`auth-tokens-001`、`auth-tokens-002`、`auth-keys-001`。

### 4.2 campaign-service.yaml 涵蓋
`campaign-campaigns-001` ~ `005`、`campaign-prizes-001`、`campaign-draws-001`。

### 4.3 inventory-service.yaml（事件契約）
無 REST paths。以 `components.schemas` 定義兩個事件 payload：
- `InventoryCommitEvent`（`drawRecordId`, `prizeId`, `quantity`）— `inventory-commit`（ADR-006/007）
- `PrizeStockConfiguredEvent`（`prizeId`, `campaignId`, `oldQuantity`, `newQuantity`, `configVersion`）— `prize-stock-configured`（ADR-010）

並以 prose 描述 consumer 冪等/排序語意（`last_config_version`、at-least-once）。

### 4.4 gateway-service.yaml（對外 surface 彙整）
彙整 auth + campaign 的**對外路徑**，標註：
- 安全方案（Bearer JWT / PUBLIC）、
- `Idempotency-Key` header（draw 路徑 required）、
- 限流 response headers（`X-RateLimit-*`）與 `429` 語意、
- 路由對映（login/register → auth-service；campaigns/draw → campaign-service）。

---

## 5. 追溯矩陣 (Traceability Matrix)

| FR / AC | Operation | Endpoint | Data Effect |
|---------|-----------|----------|-------------|
| FR-AUTH-01 / AC-AUTH-001~003 | `auth-users-001` | POST /auth/register | INSERT users |
| FR-AUTH-02 / AC-AUTH-004~006 | `auth-tokens-001` | POST /auth/login | 簽發 JWT（無 DB 寫） |
| FR-AUTH-04 / AC-AUTH-013~014 | `auth-tokens-002` | POST /auth/refresh | 簽發新 access token |
| FR-AUTH-03 / AC-AUTH-007~008 | `auth-keys-001` | GET /auth/.well-known/jwks.json | 讀取公鑰（無 DB 寫） |
| FR-CAMP-01（查）/ FR-GW-06 | `campaign-campaigns-001` | GET /campaigns | 讀 campaigns |
| FR-CAMP-01（查） | `campaign-campaigns-002` | GET /campaigns/{id} | 讀 campaign |
| FR-CAMP-01 / AC-CAMP-005 | `campaign-campaigns-003` | POST /campaigns | INSERT campaigns（DRAFT） |
| FR-CAMP-01 / FR-CAMP-11 | `campaign-campaigns-004` | PUT /campaigns/{id} | UPDATE campaigns |
| FR-CAMP-01 / AC-CAMP-005 | `campaign-campaigns-005` | PATCH /campaigns/{id}/status | 狀態轉移 |
| FR-CAMP-02~06 / AC-CAMP-001~003 | `campaign-prizes-001` | PUT /campaigns/{id}/prizes | upsert prizes + 發布 `prize-stock-configured` |
| FR-CAMP-07~19 / AC-CAMP-004~016 | `campaign-draws-001` | POST /campaigns/{id}/draw | INSERT draw_records + 計次 + 發布 `inventory-commit` |

> 每個 operation 的詳細 request/response schema 與錯誤清單，見對應 YAML。

---

## 6. 待確認的開放點 (Open Points for Review)

1. **~~campaign 詳情端點~~**（`campaign-campaigns-002`）：**已回寫** — 補入 story `ST-CAMP-010`（瀏覽活動）與 SA `UC-6`（campaign-service），隱含需求已顯式化，此開放點關閉。
2. **抽獎 response 的 `data` 形狀**：單次回傳單一結果物件、批次回傳 `{ draws: [...] }`（詳見 campaign-service.yaml）。
3. **`PATCH .../status` 的請求體**：`{ "status": "ACTIVE" }` 或專屬 action（`POST .../activate`）。預設採 `PATCH` + `{status}`。
