# 測試狀態矩陣 (Test Status Matrix)

> **以 story 出發**管理測試完成狀態（unit / integration / e2e 三層），呼應 [AGENTS.md](../../AGENTS.md) §12.4/12.5 與 [stories/README.md](../stories/README.md) §4 的 story 索引。
>
> **狀態語意**：✅ = 已覆蓋（掛驗證手段）、⬜ = 缺口、`—` = 不適用（該層不測此不變量）。
> **紀律**：狀態值由 **CI 結果推導**，不是人工手動打勾（§12.5 自驗證機制）。

## 執行方式

| 層 | 命令 | 時機 |
|----|------|------|
| unit | `./gradlew test` | 每次（秒級，dev loop） |
| integration | `./gradlew integrationTest` | CI / pre-merge（Testcontainers：Postgres/Redis/RabbitMQ） |
| e2e | `docs/stories/journeys.md` 的 J-1/J-2/J-3 | 功能全綠後 SA/QA |

---

## 矩陣

### auth-service

| Story | 不變量（AC） | Unit | Integration | E2E | 缺口 |
|-------|-------------|------|-------------|-----|------|
| ST-AUTH-001 註冊 | AC-AUTH-001/002/003 | ✅ `AuthServiceTest` | — | ⬜ J-2 | — |
| ST-AUTH-002 登入 | AC-AUTH-004/005/006 | ✅ `AuthServiceTest`、`JwtServiceTest`、`NimbusJwtVerifierTest` | ✅ `RedisTokenRegistryIT`（白名單踢最舊） | ⬜ J-2 | — |
| ST-AUTH-003 refresh | AC-AUTH-013/014 | ⬜ | — | — | Should，未實作 |
| ST-AUTH-004 權限分級 | AC-AUTH-009/010/011 | ⬜ | — | — | 補 SecurityConfig 授權測試 |

### campaign-service

| Story | 不變量（AC） | Unit | Integration | E2E | 缺口 |
|-------|-------------|------|-------------|-----|------|
| ST-CAMP-001 建立/編輯活動 | AC-CAMP-005 | ✅ `CampaignStateMachineTest` | — | ⬜ J-1 | — |
| ST-CAMP-002 配置獎品機率 | AC-CAMP-001/002/003 | ✅ `PrizeServiceValidationTest` | — | — | — |
| ST-CAMP-003 動態修改獎品 | UC-2 intent | ✅ `PrizeServiceReconcileTest` | ⬜ MQ（`prize-stock-configured` 投遞） | ⬜ J-1 | 補 MQ |
| ST-CAMP-004 單次抽獎 | AC-CAMP-004/014/016 | ✅ `DrawServiceTest` | — | ⬜ J-2 | — |
| ST-CAMP-005 批次抽獎 | AC-CAMP-008/009/010 | ✅ `DrawServiceTest` | — | — | — |
| ST-CAMP-006 並發多個單次 | AC-CAMP-011 | ⬜ | ✅ `DrawIdempotencyConcurrencyIT`（Postgres 併發撞 UNIQUE） | — | — |
| ST-CAMP-007 次數上限 | AC-CAMP-006/007 | ✅ `DrawServiceTest` | — | — | — |
| ST-CAMP-008 防重複/replay | AC-CAMP-012/013 | ✅ `DrawServiceTest` | ✅ `DrawIdempotencyConcurrencyIT`（Postgres 併發撞 UNIQUE） | ⬜ J-3 | — |
| ST-CAMP-009 防超抽（確認+降級） | AC-CAMP-014 | ✅ `DrawServiceTest` | ⬜ Redis 預扣 Lua | — | 補 Redis |
| ST-CAMP-010 瀏覽活動 | AC-GW-010 | ⬜ | — | — | 補 controller 測試 |

### inventory-service

| Story | 不變量（AC） | Unit | Integration | E2E | 缺口 |
|-------|-------------|------|-------------|-----|------|
| ST-INV-001 扣減不為負 | AC-INV-001 | ✅ `InventoryDeductionServiceTest` | ✅ `InventoryDeductionConcurrencyIT`（Postgres 併發） | — | — |
| ST-INV-002 冪等 | AC-INV-003 | ✅ `InventoryDeductionServiceTest` | ⬜ MQ at-least-once 重投 | — | 補 MQ |
| ST-INV-003 補償 | AC-INV-002 | ✅ `InventoryDeductionServiceTest` | — | ⬜ J-3 | — |
| ST-INV-004 帳目校對 | AC-INV-004/005 | ⬜ | — | — | Should，補校對測試 |

### gateway-service

| Story | 不變量（AC） | Unit | Integration | E2E | 缺口 |
|-------|-------------|------|-------------|-----|------|
| ST-GW-001 身份驗證 | AC-GW-001/002/003/004 | ✅ `NimbusJwtVerifierTest` | — | — | 補 GlobalFilter 測試 |
| ST-GW-002 身份傳遞 | AC-GW-004 | ⬜ | — | — | 補 filter 測試 |
| ST-GW-003 限流 | AC-GW-006/007 | ⬜ | ⬜ Redis 限流 Lua/計數 | — | 補 Redis |
| ST-GW-004 冪等識別檢查 | AC-GW-008/009 | ✅ `GatewayRoutesTest` | — | — | — |
| ST-GW-005 路由 | AC-GW-004/005 | ⬜ | — | — | 補路由測試 |
| ST-GW-006 公開功能 | AC-GW-010/011 | ✅ `GatewayRoutesTest` | — | — | — |

### cross-cutting

| Story | 不變量 | Unit | Integration | E2E | 缺口 |
|-------|--------|------|-------------|-----|------|
| ST-X-001 錯誤流程 | 各服務 error envelope | 部分（各 `*Test` 的 error 分支） | — | — | 補 error envelope 測試 |
| ST-X-002 RESTful/文件 | OpenAPI YAML ×4 | — | — | — | 非測試（SD 交付物） |

---

## 整合測試缺口優先序（§12.4：只測單元測不到的跨邊界不變量）

1. ⬜ **Redis：ST-CAMP-009 預扣 Lua**（campaign 抽獎熱點預扣）
2. ⬜ **MQ：ST-CAMP-003 + ST-INV-002**（`prize-stock-configured` / `inventory-commit` at-least-once + 冪等）
3. ⬜ **Redis：ST-GW-003 限流**（gateway 固定窗口計數）
