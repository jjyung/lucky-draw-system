# campaign-service — API List 總表

> 本表為 campaign-service 的 API 索引（source of truth）。API ID / operationId 與 [`../README.md`](../README.md) §3 一致；每支 API 的實作方式見 `../impl/<api-id>.md`；錯誤碼見 [`../error-list.md`](../error-list.md)。

| API ID | Method + Path | 角色 | 摘要 | 實作計畫 | 狀態 |
|--------|---------------|------|------|----------|------|
| `campaign-campaigns-001` | GET /api/v1/campaigns | PUBLIC | 活動列表（不含管理欄位） | [../impl/campaign-campaigns-001.md](../impl/campaign-campaigns-001.md) | Planned |
| `campaign-campaigns-002` | GET /api/v1/campaigns/{campaignId} | PUBLIC | 活動詳情 | [../impl/campaign-campaigns-002.md](../impl/campaign-campaigns-002.md) | Planned |
| `campaign-campaigns-003` | POST /api/v1/campaigns | ADMIN | 建立活動（回 DRAFT，201） | [../impl/campaign-campaigns-003.md](../impl/campaign-campaigns-003.md) | Planned |
| `campaign-campaigns-004` | PUT /api/v1/campaigns/{campaignId} | ADMIN | 更新活動（全量，可編輯狀態） | [../impl/campaign-campaigns-004.md](../impl/campaign-campaigns-004.md) | Planned |
| `campaign-campaigns-005` | PATCH /api/v1/campaigns/{campaignId}/status | ADMIN | 活動狀態轉移（DRAFT→ACTIVE→ENDED） | [../impl/campaign-campaigns-005.md](../impl/campaign-campaigns-005.md) | Planned |
| `campaign-prizes-001` | PUT /api/v1/campaigns/{campaignId}/prizes | ADMIN | 配置獎品與機率（整批，驗證總和=100%） | [../impl/campaign-prizes-001.md](../impl/campaign-prizes-001.md) | Planned |
| `campaign-draws-001` | POST /api/v1/campaigns/{campaignId}/draw | USER | 抽獎（單次/批次，需 Idempotency-Key） | [../impl/campaign-draws-001.md](../impl/campaign-draws-001.md) | Planned |

## 對應規格

- OpenAPI：`../openapi/campaign-service.yaml`
- SA：`../../specs/campaign-service/README.md`
- DB：`../../db/campaign-db.md`
- ADR：004（權重抽獎）、005（冪等）、006（防超抽）、010（庫存同步）、007（事件）
