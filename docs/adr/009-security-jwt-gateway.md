# ADR-009: 安全機制 — JWT + Gateway 驗證 (Security: JWT with RS256)

**Date:** 2026-08-13
**Updated:** 2026-08-14 — public key 公開採 JWKS（`/.well-known/jwks.json`）；授權表移除個人抽獎記錄、活動管理改 resource-oriented 路徑 + role 授權（統一 `/api/v1` 前綴）。
**Updated:** 2026-08-16 — Epic 3 收斂兩個 SD 開放點：① 下游複驗的公鑰**改由 Redis 共用狀態分發**（auth-service 啟動寫 `jwt:public-key`，各服務自 Redis 讀取，不 HTTP 回 auth-service）；② Gateway 驗證後**保留 `Authorization` 轉發下游**（供各服務獨立複驗，defense in depth），並同時注入 `X-User-Id`/`X-User-Roles` 作提示（原「移除 Authorization」與 §3「獨立複驗」衝突，此處以 §3 為準）。
**Updated:** 2026-08-16 — **登出/撤銷：token 白名單**。登入簽發時將 `jti` 註冊進 Redis（扁平 key `auth:token:{jti}` TTL 對齊 token 有效期，＋ per-user 集合 `auth:sessions:{userId}` ZSET）；驗證語意改為「**簽章通過 且 jti 仍在白名單**（in Redis）才通過」（fail-closed）。登出 = `POST /auth/logout` 將 jti 移出白名單，之後該 token 回 `REVOKED → 401`。**同時登入數量控管**：per-user 集合 `ZCARD` 即同時登入數，上限可配置（`lucky-draw.auth.max-sessions`，預設 5），登入時超限**踢最舊（FIFO）**。採白名單（而非黑名單）的理由：改密碼/停權需撤銷該 user **所有** token 時，黑名單無從得知要放哪些 token，除非本就 keep 所有已發 token——那即是白名單。此決策**放寬「身份憑證主動撤銷 Won't」**（requirements.md §4），代價是 Redis 成為鑑別關鍵路徑（Redis 掛 → 全部 401），取代原「無狀態」的原始理由，以換取可撤銷與 session 控管。
**Status:** Accepted

## Context

系統包含用戶端（登入/抽獎）與管理端（活動與機率配置）兩種角色，需要明確的鑑別（authentication）與授權（authorization）機制：

1. **登入憑證**：使用者密碼登入後，需要在無狀態（stateless）的微服務架構下持續識別身份。Session + 共享 storage 會成為擴展瓶頸與耦合點。
2. **Token 簽章**：多個 service 都要驗證 token（defense in depth），需要非對稱簽章讓「簽發者」與「驗證者」分離。
3. **角色隔離**：普通使用者的 draw API 與管理者的 admin API 必須分層保護。

## Decision

採用 **JWT（RS256 非對稱簽章）＋ API Gateway 統一驗證 + 各 service 獨立複驗** 的多層安全模型：

### 1. Token 簽發：Auth Service

- **auth-service** 負責登入（login / register）與 token 簽發。
- 使用 **RS256**：auth-service 持有 **private key** 簽章；各 service 只持有 **public key** 驗章。
- private key 存放於 **GCP Secret Manager**（僅 auth-service 可讀，見 ADR-008）；public key 透過 **`/.well-known/jwks.json`（JWKS）** endpoint 公開（支援多 key 輪替）。
- JWT claims 包含：`sub`（userId）、`roles`（如 `ROLE_USER` / `ROLE_ADMIN`）、`exp` / `iat` / `iss`。

### 2. Gateway 層：驗證 + 路由 + 限流

- **api-gateway**（Spring Cloud Gateway）對所有 `/api/**` 請求：
  1. **驗證** JWT 簽章與過期時間；
  2. 解出 claims，並以 **header 轉發**給下游 service：`X-User-Id`、`X-User-Roles`。**原始 `Authorization` header 保留轉發下游**，供各服務獨立複驗（§3，defense in depth）；此為對早期「移除 Authorization」描述的修訂（若移除則下游無 token 可複驗，與 §3 衝突）。`X-User-Id`/`X-User-Roles` 僅為「Gateway 已驗證」的提示，非下游身分權威來源；
  3. 執行 **rate limiting**（Redis 計數器，見 ADR-003）；
  4. 依路由規則轉發到對應 service。
- **公開 endpoint**（不需 token）：`POST /api/v1/auth/login`、`POST /api/v1/auth/register`、`GET /api/v1/campaigns`（活動列表）。

### 3. 各 Service 層：獨立複驗（Defense in Depth）

- 每個 service 都整合 **Spring Security**，並**獨立驗證 JWT 簽章**（用 public key），**不信任 Gateway 轉發的 header**——把 header 視為「Gateway 已驗證」的附加資訊，但實際身份以自行驗證的 claims 為準。
- 理由：若某個 service 被直接暴露（misconfigured route / 內網直連），沒有 Gateway 保護時，service 仍能自行鑑別請求。
- **公鑰分發（Epic 3 起）**：auth-service 啟動時將 public JWK 寫入 Redis（key `jwt:public-key`）；各服務自 Redis 讀取公鑰（本地 in-memory 短 TTL 快取 + `kid` miss 刷新），**不 HTTP 回 auth-service 取 JWKS**。JWKS endpoint（`auth-keys-001`）仍公開（外部相容），但下游複驗不依賴它。驗證邏輯為 common module 的共用 `JwtVerifier`（RS256 + `exp` + `iss` → `sub`/`roles`）。

### 4. 授權（Authorization）

| Endpoint | 角色 | 說明 |
|----------|------|------|
| `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | PUBLIC | 註冊 / 登入 |
| `GET /api/v1/campaigns` | PUBLIC | 活動列表（不含管理欄位） |
| `POST /api/v1/campaigns/{id}/draw` | `ROLE_USER`（需 token） | 抽獎（另需 Idempotency-Key，見 ADR-005） |
| `POST/PUT /api/v1/campaigns` 與其子資源（獎品/機率配置、狀態管理） | `ROLE_ADMIN` | 活動與獎品管理（resource-oriented 路徑，以 claims 含 ADMIN 授權） |

- 授權判定以 **JWT claims 的 roles** 為準；service 內以 Spring Security method security（`@PreAuthorize("hasRole('ADMIN')")`）落實。

## Consequences

**正面：**

- **無狀態（stateless）鑑別**：token 自帶身份與角色，任何 service instance 都可獨立驗證，水平擴展零 session 同步成本。
- **非對稱簽章的安全性**：private key 只存在簽發端，即使某個下游 service 被入侵，也無法偽造 token（只有 public key 被竊取，無法簽章）。
- **多層防禦**：Gateway 擋在最外層 + 每 service 獨立複驗，單層設定失誤不會直接造成未授權存取。
- **角色模型清晰**：PUBLIC / USER / ADMIN 三層，與 API 權限表一一對應，易於稽核。

**負面 / 需付出的代價：**

- **Token 無法主動撤銷（除非黑名單）**：JWT 在到期前有效；登出 / 停權需要額外的 Redis blacklist（如 `jwt:blacklist:{jti}`）機制，POC 階段可先以短 TTL（如 30–60 分鐘）+ 密碼重設時增加 `token_version` 欄位處理。
- **public key 的分發**：各 service 需要定期輪換（rotate）public key；JWKS 方案可支援多 key 輪替，需在實作時納入。
- **header 轉發的信任邊界**：`X-User-Id` header 可能被直接呼叫 service 的人偽造，因此「獨立複驗」是必要而非可選（已在決策中強制）。
- **每個 service 都要接 Spring Security + JWT filter**：重複設定較多，建議在 `common` 或各 service 的 `SecurityConfig` 統一 template 化。

## Alternatives

- **Session + Redis session store**：可主動撤銷，但每次請求都要查 Redis、service 間共享 session storage，stateless 擴展較差，且 Gateway 無法純粹地路由（需 session affinity），否決。
- **Symmetric JWT（HS256 共享密鑰）**：所有 service 共享同一密鑰，任一 service 被入侵即可偽造 token；非對稱 RS256 的隔離性明顯較佳，否決。
- **OAuth2 / OIDC 完整授權流程（Authorization Server + Resource Server）**：Spring Security 原生支援且更標準化，但引入更重的授權協定（redirect flow、client 註冊）；POC 階段用簡化版的「Auth Service 簽發 JWT」即可，OAuth2 留作 prod 演化方向。
- **Gateway 驗證後下游不再複驗（信任 header）**：最省事，但違反 defense in depth，service 被直連時無保護，否決。
