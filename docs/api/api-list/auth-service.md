# auth-service — API List 總表

> 本表為 auth-service 的 API 索引（source of truth）。API ID / operationId 與 [`../README.md`](../README.md) §3 一致；每支 API 的實作方式見 `../impl/<api-id>.md`；錯誤碼見 [`../error-list.md`](../error-list.md)。

| API ID | Method + Path | 角色 | 摘要 | 實作計畫 | 狀態 |
|--------|---------------|------|------|----------|------|
| `auth-users-001` | POST /api/v1/auth/register | PUBLIC | 使用者註冊（預設 ROLE_USER，密碼不可逆儲存） | [../impl/auth-users-001.md](../impl/auth-users-001.md) | Planned |
| `auth-tokens-001` | POST /api/v1/auth/login | PUBLIC | 登入並簽發身份憑證（JWT RS256，jti 註冊白名單） | [../impl/auth-tokens-001.md](../impl/auth-tokens-001.md) | Planned |
| `auth-tokens-002` | POST /api/v1/auth/refresh | USER（Should） | 刷新存取憑證（身份/角色不變） | [../impl/auth-tokens-002.md](../impl/auth-tokens-002.md) | Planned |
| `auth-tokens-003` | POST /api/v1/auth/logout | USER | 登出並撤銷憑證（jti 移出白名單，ADR-009 修訂） | — | Planned |
| `auth-keys-001` | GET /api/v1/auth/.well-known/jwks.json | PUBLIC | 取得驗證公鑰（JWKS，多 key 輪替） | [../impl/auth-keys-001.md](../impl/auth-keys-001.md) | Planned |

## 對應規格

- OpenAPI：`../openapi/auth-service.yaml`
- SA：`../../specs/auth-service/README.md`
- DB：`../../db/auth-db.md`
- ADR：009（JWT RS256）、002（Database-per-Service）
