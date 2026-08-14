# gateway-service — API List 總表

> gateway-service **無自有業務 API**（唯一對外入口，驗證→限流→冪等檢查→路由→身份傳遞，不轉寫 payload）。故本服務沒有自有 `api-id`；其對外 surface 為 auth + campaign 的彙整，見下表。

| 對外路徑（彙整） | 安全 | Gateway 行為 | 路由至 |
|------------------|------|--------------|--------|
| POST /api/v1/auth/register、/login | PUBLIC | 限流（A0500） | auth-service |
| GET /api/v1/auth/.well-known/jwks.json | PUBLIC | 限流 | auth-service |
| POST /api/v1/auth/refresh | bearerAuth | 驗證 + 限流 | auth-service |
| GET /api/v1/campaigns、/campaigns/{id} | PUBLIC | 限流 | campaign-service |
| POST/PUT/PATCH /api/v1/campaigns…、prizes | bearerAuth（ADMIN） | 驗證 + 限流 | campaign-service |
| POST /api/v1/campaigns/{id}/draw | bearerAuth（USER） | 驗證 + 限流 + **Idempotency-Key 檢查**（缺→A0501） | campaign-service |

## 對應規格

- OpenAPI（surface 彙整）：`../openapi/gateway-service.yaml`
- SA：`../../specs/gateway-service/README.md`
- ADR：009（JWT 驗證/路由）、003（限流）、005（冪等 header 檢查）
- 錯誤碼：`A0202/A0203`（401）、`A0500`（429 限流）、`A0501`（400 缺 Idempotency-Key）
