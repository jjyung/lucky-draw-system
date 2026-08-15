# 使用情境 (User Stories)

> 以「角色 × 價值」視角描述系統行為的**情境層**文件，作為需求清單（`docs/specs/requirements.md`）與 per-service SA 規格（`docs/specs/<service>/README.md` 的 UC/AC）之間的橋樑。

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-15 |
| **角色 (Layer)** | SA — 使用情境（業務行為與價值，非技術實作） |
| **上游依據** | [requirements.md](../specs/requirements.md)（天條 §0 + FR/NFR） |
| **下游對應** | [auth-service](../specs/auth-service/README.md)、[campaign-service](../specs/campaign-service/README.md)、[inventory-service](../specs/inventory-service/README.md)、[gateway-service](../specs/gateway-service/README.md) |

## 2. 目的與紀律

1. **有所本**：每個 story 標注其**依賴需求（`FR-*` / `NFR-*`）**與**天條依據（§0 原文段落）**。凡天條 §0 未明列的能力，一律**不新增 story**。
2. **有依賴**：story → 需求 → UC → AC 的追溯鏈完整閉環（見 §5 追溯矩陣）。
3. **分層**：本目錄只寫「誰（角色）、要什麼（價值）、為何（目的）與驗收意圖（acceptance intent）」。API 路由、資料表、併發演算法、憑證簽發實作等**技術實作**屬 SD 層，不在本層。
4. **不牴觸**：任何敘述與天條 §0 原文牴觸時，以天條為準。

### Story 模板（每條必備欄位）

```markdown
### ST-XXX-NNN — <標題>

- **User Story:** As a <角色>, I want to <能力>, so that <價值/目的>。
- **Priority:** Must / Should / Could
- **依賴需求 (Depends on):** `FR-*` / `NFR-*`
- **天條依據 (Source):** §0「<原文對應段落>」
- **對應規格:** UC-x（`<service>/README.md`）
- **驗收意圖 (Acceptance intent):** 對應 AC-*（GIVEN/WHEN/THEN 摘要）
```

---

## 3. 使用者旅程 (User Journeys)

> 端到端敘事見 [journeys.md](journeys.md)，揭露 FR 列表無法單獨表達的隱含需求（如活動詳情端點）。SD 應先讀旅程，再讀下方 story 索引。

| Journey | 角色 | 關鍵缺口 |
|---------|------|----------|
| [J-1](journeys.md#j-1) | ADMIN | 動態改數量需同步庫存（ADR-010） |
| [J-2](journeys.md#j-2) | USER | **活動詳情端點**（開放點 #1） |
| [J-3](journeys.md#j-3) | USER/平台 | 補償告警可觀察性（NFR-06） |

---

## 4. Story 索引

| Story ID | 標題 | 服務 | 優先級 |
|----------|------|------|--------|
| [ST-AUTH-001](auth-service.md#st-auth-001) | 使用者註冊 | auth | Must |
| [ST-AUTH-002](auth-service.md#st-auth-002) | 使用者登入取得身份憑證 | auth | Must |
| [ST-AUTH-003](auth-service.md#st-auth-003) | 登入有效期延續 | auth | Should |
| [ST-AUTH-004](auth-service.md#st-auth-004) | 權限分級 | auth | Must |
| [ST-CAMP-001](campaign-service.md#st-camp-001) | 建立／編輯抽獎活動 | campaign | Must |
| [ST-CAMP-002](campaign-service.md#st-camp-002) | 配置獎品與機率（含銘謝惠顧） | campaign | Must |
| [ST-CAMP-003](campaign-service.md#st-camp-003) | 動態修改獎品內容 | campaign | Must |
| [ST-CAMP-004](campaign-service.md#st-camp-004) | 單次抽獎 | campaign | Must |
| [ST-CAMP-005](campaign-service.md#st-camp-005) | 批次抽獎 | campaign | Must |
| [ST-CAMP-006](campaign-service.md#st-camp-006) | 並發多個單次抽獎 | campaign | Must |
| [ST-CAMP-007](campaign-service.md#st-camp-007) | 個人抽獎次數上限 | campaign | Must |
| [ST-CAMP-008](campaign-service.md#st-camp-008) | 防重複抽獎與 replay | campaign | Must |
| [ST-CAMP-009](campaign-service.md#st-camp-009) | 防超抽（庫存確認＋降級銘謝惠顧） | campaign | Must |
| [ST-CAMP-010](campaign-service.md#st-camp-010) | 瀏覽活動（列表＋詳情） | campaign | Must |
| [ST-INV-001](inventory-service.md#st-inv-001) | 執行庫存扣減（不為負） | inventory | Must |
| [ST-INV-002](inventory-service.md#st-inv-002) | 扣減冪等（同一筆中獎只扣一次） | inventory | Must |
| [ST-INV-003](inventory-service.md#st-inv-003) | 庫存不足補償（撤銷＋校正＋告警） | inventory | Must |
| [ST-INV-004](inventory-service.md#st-inv-004) | 定期帳目校對 | inventory | Should |
| [ST-GW-001](gateway-service.md#st-gw-001) | 身份驗證（憑證有效性與時效） | gateway | Must |
| [ST-GW-002](gateway-service.md#st-gw-002) | 身份傳遞給下游 | gateway | Must |
| [ST-GW-003](gateway-service.md#st-gw-003) | 請求頻率限制 | gateway | Must |
| [ST-GW-004](gateway-service.md#st-gw-004) | 抽獎請求冪等識別檢查 | gateway | Must |
| [ST-GW-005](gateway-service.md#st-gw-005) | 請求路由 | gateway | Must |
| [ST-GW-006](gateway-service.md#st-gw-006) | 公開功能免憑證 | gateway | Must |
| [ST-X-001](#st-x-001) | 完整錯誤流程與輸入驗證 | cross-cutting | Must |
| [ST-X-002](#st-x-002) | RESTful 風格與 API 文件 | cross-cutting | Must |

---

## 5. Cross-cutting Stories

> 跨服務交錯需求，不隸屬單一服務，故置於索引檔。

### ST-X-001 — 完整錯誤流程與輸入驗證

- **User Story:** As a 前端／整合方（client developer）, I want to 收到語意明確且一致的錯誤回應（`400`/`401`/`403`/`404`/`409`/`429`/`500`）與輸入驗證結果, so that 我能正確處理失敗並引導使用者。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-X-01`
- **天條依據 (Source):** §0「完整的錯誤流程處理與輸入驗證」
- **對應規格:** 各服務 UC 之 validation/error 路徑；`docs/api/error-list.md`
- **驗收意圖 (Acceptance intent):**
  - 非法輸入 → `400`/`422`，錯誤可理解，且不產生副作用。
  - 未驗證/無效憑證 → `401`；越權 → `403`；不存在資源 → `404`；狀態衝突 → `409`；超限 → `429`。
  - 錯誤回應格式全系統一致（見 error-list.md）。

### ST-X-002 — 前後端分離、RESTful 風格與 API 文件

- **User Story:** As a 前端／整合方, I want to 以清晰且一致的 RESTful 路由與參數說明存取功能，並有完整的 API 文件, so that 我能穩定整合、降低溝通成本。
- **Priority:** Must
- **依賴需求 (Depends on):** `FR-X-03`, `FR-X-04`, `NFR-08`
- **天條依據 (Source):** §0「前後端分離，後端採用 RESTful API 風格，並提供清晰的路由與參數說明」「API 說明文件」
- **對應規格:** `docs/api/openapi/*.yaml`、`docs/api/impl/*.md`
- **驗收意圖 (Acceptance intent):**
  - 所有 API 有對應 OpenAPI 3.0 規格與路由說明。
  - 前端與後端可依契約並行開發。

---

## 6. 追溯矩陣 (Traceability Matrix)

> story → 需求（FR/NFR）→ 天條 → UC → AC → **API/事件** 的完整閉環。**若某 FR 無對應 story，代表情境層有缺口；若某 story 無對應 API/事件，代表 SD 未滿足 story。**

| Story ID | 依賴需求 (FR/NFR) | 對應 UC | 對應 AC | 對應 API/事件 |
|----------|-------------------|---------|---------|---------------|
| ST-AUTH-001 | FR-AUTH-01, FR-AUTH-06, FR-X-01 | UC-1 | AC-AUTH-001/002/003 | `auth-users-001` |
| ST-AUTH-002 | FR-AUTH-02, FR-AUTH-03, FR-X-01 | UC-2 | AC-AUTH-004/005/006/007/008/012 | `auth-tokens-001`, `auth-keys-001` |
| ST-AUTH-003 | FR-AUTH-04 | UC-3 | AC-AUTH-013/014 | `auth-tokens-002` |
| ST-AUTH-004 | FR-AUTH-05, FR-X-01 | UC-4 | AC-AUTH-009/010/011 | 各 ADMIN 端點授權（無獨立 API） |
| ST-CAMP-001 | FR-CAMP-01, FR-CAMP-11, FR-X-01 | UC-1, UC-3 | AC-CAMP-005 | `campaign-campaigns-003/004/005` |
| ST-CAMP-002 | FR-CAMP-02, FR-CAMP-03, FR-CAMP-04, FR-CAMP-06 | UC-2 | AC-CAMP-001/002/003 | `campaign-prizes-001` |
| ST-CAMP-003 | FR-CAMP-05 | UC-2 | UC-2 acceptance intent | `campaign-prizes-001` + `prize-stock-configured` |
| ST-CAMP-004 | FR-CAMP-07, FR-CAMP-10, FR-CAMP-17, FR-CAMP-18, FR-CAMP-19, FR-X-01 | UC-4 | AC-CAMP-004/014/016 | `campaign-draws-001` + `inventory-commit` |
| ST-CAMP-005 | FR-CAMP-08, FR-CAMP-10, FR-CAMP-15, FR-CAMP-17, FR-CAMP-18, FR-CAMP-19, FR-X-02 | UC-5 | AC-CAMP-008/009/010 | `campaign-draws-001`（count=N）+ `inventory-commit` |
| ST-CAMP-006 | FR-CAMP-09 | UC-4 | AC-CAMP-011 | `campaign-draws-001`（並發多請求） |
| ST-CAMP-007 | FR-CAMP-11, FR-CAMP-12 | UC-1, UC-4, UC-5 | AC-CAMP-006/007 | `campaign-draws-001`（超限 429） |
| ST-CAMP-008 | FR-CAMP-13, FR-CAMP-14 | UC-4, UC-5 | AC-CAMP-012/013 | `campaign-draws-001`（replay）+ gateway Idempotency-Key 檢查 |
| ST-CAMP-009 | FR-CAMP-18, FR-CAMP-19 | UC-4, UC-5 | AC-CAMP-014 | `campaign-draws-001`（庫存確認+降級） |
| ST-CAMP-010 | FR-CAMP-01（R）, FR-GW-06 | UC-6 | AC-GW-010 | `campaign-campaigns-001/002` |
| ST-INV-001 | FR-INV-01, FR-INV-02 | UC-1 | AC-INV-001 | `inventory-commit` 事件 |
| ST-INV-002 | FR-INV-04 | UC-1 | AC-INV-003 | `inventory-commit`（`drawRecordId` 去重） |
| ST-INV-003 | FR-INV-03 | UC-2 | AC-INV-002 | 補償（無對外 API，內部撤銷+告警） |
| ST-INV-004 | FR-INV-05 | UC-3 | AC-INV-004/005 | 帳目校對（無對外 API，內部排程） |
| ST-GW-001 | FR-GW-01 | UC-1 | AC-GW-001/002/003/004 | gateway surface（驗證） |
| ST-GW-002 | FR-GW-02 | UC-1 | AC-GW-004 | gateway surface（`X-User-Id`/`X-User-Roles` 傳遞） |
| ST-GW-003 | FR-GW-03 | UC-2 | AC-GW-006/007 | gateway surface（限流 429） |
| ST-GW-004 | FR-GW-04 | UC-3 | AC-GW-008/009 | gateway surface（Idempotency-Key 檢查） |
| ST-GW-005 | FR-GW-05 | UC-1 | AC-GW-004/005 | gateway surface（路由） |
| ST-GW-006 | FR-GW-06 | UC-4 | AC-GW-010/011 | gateway surface（公開功能放行） |
| ST-X-001 | FR-X-01 | 各服務 UC | 各服務 AC（error） | `error-list.md` |
| ST-X-002 | FR-X-03, FR-X-04, NFR-08 | —（SD） | — | OpenAPI YAML ×4 |

### FR 覆蓋檢查

| 需求群組 | 是否被 story 覆蓋 |
|----------|-------------------|
| FR-GW-01 ~ FR-GW-06 | ✅ ST-GW-001 ~ ST-GW-006 |
| FR-AUTH-01 ~ FR-AUTH-06 | ✅ ST-AUTH-001 ~ ST-AUTH-004 |
| FR-CAMP-01 ~ FR-CAMP-19（含事件發布） | ✅ ST-CAMP-001 ~ ST-CAMP-009 |
| FR-INV-01 ~ FR-INV-05 | ✅ ST-INV-001 ~ ST-INV-004 |
| FR-X-01 ~ FR-X-04 | ✅ ST-X-001, ST-X-002 |
| NFR-07（測試）、NFR-08（API 文件） | ✅ 見 §6 註記 |

---

## 7. 非功能需求 (NFR) 對應註記

> NFR 是系統性約束，不適合逐一拆成「誰、要什麼」的 user story；於此註記其承載方式。

| NFR | 承載位置 |
|-----|----------|
| NFR-01 高可用 / NFR-02 水平擴展 / NFR-04 多環境配置 | 架構層（ADR-008、deployment.md），不拆 story |
| NFR-03 高併發一致性 | ST-CAMP-007/008/009、ST-INV-001/002/003 的驗收意圖中體現 |
| NFR-05 安全性 | ST-AUTH-001/002/004、ST-GW-001/002 的驗收意圖中體現 |
| NFR-06 可觀察性 | ST-INV-003（告警）、ST-INV-004（校對記錄） |
| NFR-07 測試覆蓋 | 各 story 的驗收意圖（AC）即為測試依據 |
| NFR-08 API 文件 | ST-X-002 |

---

## 8. 範圍邊界（不新增的 Story）

> 以下天條 §0 **未明列**的能力，**刻意不建立 story**（遵循「不擴增需求」原則）：

| 排除項目 | 原因 |
|----------|------|
| 保底機制（N 抽必中） | 天條未列，不增補 |
| 即時動態機率調整 | 天條未列 |
| 活動暫停態（PAUSED） | 天條只列 DRAFT/ACTIVE/ENDED |
| 個人抽獎記錄查詢 API | 天條未列 |
| 密碼重設／Email 驗證／2FA／Social login | 天條未列（見 auth-service spec §7） |
| OAuth2/OIDC 完整授權、憑證主動撤銷 | requirements §4 Won't |
| 金流／發券整合、前端應用、實際 prod 部署 | requirements §4 Won't |
