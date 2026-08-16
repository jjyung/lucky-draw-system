# Roadmap（交付進度）

> 本專案的交付切分採用 **epic → story → task** 階層（見 [AGENTS.md](AGENTS.md) §14 溝通詞彙）。
> 測試狀態見 [docs/testing/test-matrix.md](testing/test-matrix.md)，由 CI 結果推導，非手動打勾。

## 已完成（pushed to main）

| Epic | 內容 | 測試 |
|------|------|------|
| **Epic 0** | Gradle 多模組骨架（common / contracts / auth / campaign / inventory / gateway） | — |
| **Epic 1** | auth-service：註冊、登入（RS256 JWT 簽發）、JWKS 公開 | unit ✅ |
| **Epic 2** | campaign-service：活動/獎品管理（狀態機、機率驗證）、權重抽獎（單次/批次）、冪等/replay、防超抽降級 | unit ✅ |
| **Epic 3** | inventory-service（事件消費：條件扣減/冪等/補償/校對）、api-gateway（JWT 複驗/限流/冪等檢查/路由）、campaign 收尾（StreamBridge/授權/prize reconcile）、**token 白名單 + 登出 + per-user session 上限** | unit ✅ + integration ✅（Postgres/Redis/test-binder） |

**測試現況**（詳見 [test-matrix.md](testing/test-matrix.md)）：
- unit：16 個測試類（TDD 不變量）
- integration：6 類 9 條（Postgres 防超抽/冪等併發、Redis 預扣/踢最舊/限流、MQ 雙事件投遞 — **test-binder 驗 binding 接線**）
- E2E（smoke）：**真 RabbitMQ** 投遞由 [smoke-test.ps1](../scripts/smoke-test.ps1) 驗證（含「抽獎後庫存扣減」Redis 斷言，實測通過）

## 未完成（依優先序）

| 項目 | 優先 | 說明 |
|------|------|------|
| Refresh token（`auth-tokens-002`） | Should | 目前回 501，未實作 |
| JUnit + 真 RabbitMQ 整合測試 | 低 | 真 broker 投遞已由 smoke-test.ps1 驗證；`InventoryEventDeliveryIT` 用 test-binder（`integrationTest` 未涵蓋真 broker）。要 CI 也驗真 broker 再補 |
| E2E 正式回歸 | 中 | 功能全綠後由 SA/QA 依 journeys J-1/J-2/J-3 執行 |
| HTTP method 錯誤 → 405 | 低 | 目前 `handleUnexpected` 把 `HttpRequestMethodNotSupportedException` 兜成 500 |
| Go-prod | 不做 | Cloud Run/Secret Manager/固定 PEM（ADR-008 已文件化，非 POC 範圍） |

## Out of Scope（POC 不做，見 requirements.md §4）

前端應用、金流/發券整合、OAuth2/OIDC 完整授權、身份憑證黑名單/停權、實際 prod 部署。
