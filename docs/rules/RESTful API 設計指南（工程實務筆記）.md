# RESTful API 設計指南（工程實務筆記）

# 一、REST 的核心觀念（先釐清本質）

REST = Representational State Transfer\
是一種「架構風格」，不是協議也不是標準。

核心思想只有一句：

> 以資源（Resource）為中心設計 API，而不是動作（Action）

---

# 二、資源（Resource）設計原則（最重要）

## 2\.1 一切皆資源

常見資源：

- user

- order

- book

- payment

- report

錯誤（動作導向）：

```
/getUser
/createOrder
/updateProfile

```

正確（資源導向）：

```
/users
/orders
/profiles

```

---

## 2\.2 Resource 命名規範（強烈建議）

### 原則：URL 使用「複數名詞」

```
/users
/orders
/books

```

原因：

- 表示資源集合（collection）

- 語意清晰

- 符合主流 API 設計（GitHub / Stripe / Google）

---

## 2\.3 Domain vs URL 命名一致性（DDD 友善）

| 層級 | 命名建議 | 
|---|---|
| URL | /users | 
| Entity | User | 
| Controller | UserController | 
| DTO | GetUserResponseDTO | 

建議：

> URL 複數 + Domain Model 單數（最佳實務）

---

# 三、HTTP Method 設計規範（語意必須正確）

## 3\.1 標準 Method 對應

| Method | 語意 | 用途 | 是否冪等 | 
|---|---|---|---|
| GET | 讀取 | 查詢資源 | ✔ | 
| POST | 建立 | 新增資源 | ✘ | 
| PUT | 完整更新 | 覆蓋資源 | ✔ | 
| PATCH | 部分更新 | 局部修改 | ✘ | 
| DELETE | 刪除 | 移除資源 | ✔ | 

---

## 3\.2 標準 RESTful Endpoint 範例

```
GET    /users          # 查詢列表
GET    /users/{id}     # 查詢單一資源
POST   /users          # 建立資源
PUT    /users/{id}     # 全量更新
PATCH  /users/{id}     # 部分更新
DELETE /users/{id}     # 刪除資源

```

---

# 四、URL 設計最佳實踐（企業級標準）

## 4\.1 基本命名規則

建議：

- 全小寫

- 使用 `-`（kebab-case）

- 不使用動詞

- 不使用底線 `_`

正確：

```
/user-orders
/order-items

```

錯誤：

```
/getUserOrders
/user_orders

```

---

## 4\.2 階層式資源（Nested Resource）

```
/users/{userId}/orders
/orders/{orderId}/items

```

適用情境：

- 強關聯資源

- 明確父子關係

避免過深：

```
/users/{id}/orders/{id}/items/{id}/details  ❌（過深）

```

建議最多 2\~3 層。

---

# 五、Query Parameter 設計（查詢標準）

## 5\.1 分頁（必備）

```
GET /users?page=1&size=20

```

或：

```
GET /users?offset=0&limit=20

```

企業推薦回傳格式：

```
{
  "data": [...],
  "page": 1,
  "size": 20,
  "total": 100
}

```

---

## 5\.2 篩選 / 排序

```
GET /orders?status=PAID
GET /users?sort=createdAt,desc
GET /products?category=book&priceMin=100

```

---

# 六、HTTP Status Code 設計（很多團隊做錯）

## 6\.1 成功回應

| 狀態碼 | 使用情境 | 
|---|---|
| 200 OK | 一般成功 | 
| 201 Created | 成功建立資源 | 
| 204 No Content | 成功但無回傳內容 | 

---

## 6\.2 錯誤回應（標準化）

| 狀態碼 | 說明 | 
|---|---|
| 400 | 參數錯誤 | 
| 401 | 未授權 | 
| 403 | 權限不足 | 
| 404 | 資源不存在 | 
| 409 | 資源衝突 | 
| 429 | 請求過於頻繁（限流／抽獎次數超限） | 
| 500 | 系統錯誤 | 

嚴禁：

```
永遠回 200 + errorCode  ❌（反 REST）

```

---

# 七、Request / Response 設計規範（配合 OpenAPI）

## 7\.1 DTO 命名

建議統一採 `{HttpMethod}{Resource}RequestDTO` / `{HttpMethod}{Resource}ResponseDTO` 格式；**單複數依 API 語意而定**（列表用複數、單一資源用單數、action 端點以動作命名），詳見《OpenAPI Contract Rules》：

```
CreateUserRequestDTO
GetUserResponseDTO
UpdateOrderRequestDTO

```

優點：

- 可讀性高

- 適合 SDK 生成

- 避免 Model 混淆

---

## 7\.2 回應格式標準化（全系統統一 envelope）

成功與失敗**一律使用相同 envelope**，不分兩套結構；成功時亦可於 `message` 補充說明：

```
{
  "code": "00000",
  "message": "OK",
  "data": {}
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `code` | string | 數字錯誤碼（成功 `00000`；其餘見《錯誤碼》），全系統統一 |
| `message` | string | 人類可讀訊息；成功時亦可補充說明（如批次結果摘要） |
| `data` | object / array / null | 成功承載資源；失敗為 `null` 或省略 |

- 錯誤的 HTTP status 仍依 §6.2 標準（400/401/403/404/409/429/500），`code` 與 status 對映見《錯誤碼》。
- 禁止「只回 HTTP status 不帶 `code`」；禁止成功失敗用不同結構。

---

# 八、版本控制（Versioning Strategy）

## 8\.1 URL Version（最常見）

```
/api/v1/users
/api/v2/users

```

優點：

- 清晰
- 文件友善
- 適合 OpenAPI

> 本系統前綴統一為 **`/api/v1`**（`/api` + `/v{n}` 兩段），所有端點皆在此前綴下（對應 ADR-009）。

---

## 8\.2 Header Version（進階）

```
Accept: application/vnd.company.v1+json

```

適用：

- 公開 API 平台

- 長期維護產品

---

# 九、錯誤處理設計（強烈建議統一）

錯誤回應**沿用 §7.2 的統一 envelope**，不另立格式：

```
{
  "code": "A0201",
  "message": "帳號或密碼錯誤",
  "data": null
}
```

- `code` 為**數字錯誤碼**，全系統統一管理，見《錯誤碼》（成功 `00000`；用戶端錯誤 `Axxxx`；系統錯誤 `Bxxxx`；第三方錯誤 `Cxxxx`）。
- HTTP status 與 `code` 的對映以《錯誤碼》為準，依 §6.2 標準（400/401/403/404/409/429/500）。
- 可觀測性：建議於日誌／APM 記錄 `traceId`（correlation id），但**不強制**放進 response body（若需，可於 envelope 增加 `traceId` 欄位）。

---

# 十、安全設計（企業 API 必備）

建議：

- 使用 HTTPS（強制）

- OAuth2 / JWT

- Rate Limiting

- Idempotency Key（支付類 API）

例如：

```
POST /payments
Idempotency-Key: 123456

```

---

# 十一、快取與效能（進階 REST 原則）

使用 HTTP Header：

```
Cache-Control
ETag
Last-Modified

```

適合：

- 查詢型 API

- 高頻讀取服務

- CDN 架構

---

# 十二、常見反模式（架構師必避免）

## ❌ 1. 用 POST 做所有操作

```
POST /getUser  （錯）

```

## ❌ 2. 動詞當資源名（RPC 風格 URL）

```
/createUser
/getUserOrders
/updateOrder

```

> **例外 — action 子資源**：非 CRUD 的業務動作（如抽獎、啟用、結算）無法自然映射到 CRUD verb 時，可用 `POST /resource/{id}/{action}`（如 `POST /campaigns/{id}/draw`）。這與「動詞當資源名」是不同模式：`createUser` 是「把動詞塞進資源名」（錯誤），`POST .../draw` 是「對既有資源施加動作」（合法）。需要更多情境時，動作命名多一點描述是合理的。

## ❌ 3. 不使用 HTTP Status Code

（全部回 200）

## ❌ 4. 無狀態被破壞（Session 依賴）

這會嚴重影響：

- 水平擴展

- 雲端部署

- 負載平衡

ref:

- [理解RESTful架构](https://www.ruanyifeng.com/blog/2011/09/restful.html?utm_source=chatgpt.com)

- [RESTful API 设计指南](https://www.ruanyifeng.com/blog/2014/05/restful_api.html?utm_source=chatgpt.com)