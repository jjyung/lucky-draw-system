# inventory-service — API List 總表

> inventory-service **無對外 REST API**（純後端協作服務，靠 event 驅動，ADR-006/007/010）。故本服務沒有 `api-id` 清單；其契約為**事件（message）**，見下表。

| 事件（Binding） | 方向 | Payload Schema | 冪等/排序 | 對應文件 |
|-----------------|------|----------------|-----------|----------|
| `inventory-commit` | campaign-service → inventory-service | `InventoryCommitEvent`（drawRecordId, prizeId, quantity） | `drawRecordId` UNIQUE 去重 | ADR-006/007 |
| `prize-stock-configured` | campaign-service → inventory-service | `PrizeStockConfiguredEvent`（prizeId, campaignId, oldQuantity, newQuantity, configVersion） | `prizeId` upsert + `last_config_version` 排序 | ADR-010 |

## 對應規格

- 事件契約：`../openapi/inventory-service.yaml`（`paths: {}`，僅 `components.schemas`）
- SA：`../../specs/inventory-service/README.md`
- DB：`../../db/inventory-db.md`（`inventory`、`reservations`，含 `last_config_version`）
- 消費語意（冪等/條件更新/補償）：見 `../../db/inventory-db.md` §3.3/§3.4/§3.6 與 ADR-006/010
