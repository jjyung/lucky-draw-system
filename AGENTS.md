# AGENTS.md — 系統開發流程指引

> 本文件抽取自「Design Evidence Pack」方法論（Figma-to-Code 之前先對齊需求與技術規格），並**忽略前端相關**，聚焦後端／系統開發流程。目標：在寫 code 之前，先把輸入整理成有來源、可追蹤的規格，再依角色轉換成可實作的契約。
>
> 本文件與本 repo 既有的 dev-flow（`docs/adr/` + `docs/specs/`）互補：ADR 記錄架構決策，specs 記錄需求與契約，本文件定義「如何從模糊輸入走到可追蹤規格」的紀律。

---

## 1. 核心流程

```
需求 / 設計輸入 → Design Evidence Pack → SA 業務需求 → SD 技術設計 → 實作契約 → 交付與驗證
```

| 角色 | 職責 | 交付物 |
|------|------|--------|
| **SA (System Analyst)** | 整理業務目標、行為與語意 | Business Requirements、business state、business data dictionary |
| **SD (System Designer)** | 定義系統邊界、API、DB、效能與安全契約 | Technical Design、API contract、DB design、NFR |
| **Backend** | 依契約實作 API、domain logic、persistence | code、migration、API/contract tests |
| **QA** | 驗證執行與結果整理 | test result、verification report、defect report |

SA 回答「系統要提供什麼行為」，SD 回答「系統如何提供這個能力」，後端回答「結果如何被正確、有效率、安全地取得」。三者不混在一起。

---

## 2. 先盤點 Evidence，再談規格

輸入（需求文件、設計稿、screenshot、prototype、口頭描述）的共同問題：**能產生線索，不能自動補齊 business semantics 與 production contract**。

第一個產物應是 **Design Evidence Pack**：把材料整理成可引用的證據，**把「看得到的」和「推測出來的」分開**。code 留到後面。

### 三類證據

- **結構**：resource、entity、route、狀態機、資料欄位。
- **行為與狀態**：loading、empty、error、success、permission denied、validation、confirmation、retry、duplicate submission。
- **約束**：欄位合法值、資料來源、權限、效能與可用性門檻。

### 三種標記（關鍵紀律）

| 標記 | 意義 |
|------|------|
| `observed` | 輸入中明確可確認的內容 |
| `inferred` | 從線索合理推測的內容 |
| `unknown` | 目前無法判斷、或尚未完成決策 |

> 這個區分**避免把推測直接寫成需求或技術決策**。推測必須在後續 SA/SD 階段被確認或否決。

### Evidence 最小格式

```
EVID-001
source: <需求/設計/文件來源與位置>
type: entity | api | state | rule | constraint
observed: 可確認的內容
inferred: 合理推測的內容
unknown: 無法判斷、待決策
confidence: high | medium | low
related: 相關的 entity / 規則 / 決策
```

---

## 3. SA：定義業務行為與語意

SA 文件回答四件事：

1. 這個功能服務什麼 **problem、goal、actor 與 user role**？
2. 提供哪個 **use case / business capability**？
3. 有哪些 **business rule、business state、scope 與 out-of-scope**？
4. 欄位代表什麼業務資料，以及什麼結果算符合目的（**acceptance intent**）？

### 範例：以一個 operation 為例

```
Use case: 建立 resource
Actor: 具備 resource:create 權限的使用者
Precondition: 必填欄位合法，且使用者屬於可操作的組織
Main flow:
  1. 使用者提交輸入
  2. 系統驗證輸入
  3. 系統建立 resource
  4. 系統回傳成功結果
Business rule:
  - 同一個 external reference 不可重複建立
  - 建立失敗時不得產生半完成資料
Acceptance intent:
  - 合法輸入可成功建立
  - 重複 reference 顯示可理解的錯誤
  - request 重送不會重複建立 resource
```

> API 與資料庫設計**留到 SD 階段**，SA 不先跳進技術細節。

### Business Data Dictionary（欄位的業務意義）

| 欄位 | 業務意義 | 必填 | 合法值 | 真實來源 | 敏感性 |
|------|----------|------|--------|----------|--------|
| status | 資源目前的業務狀態 | 是 | draft／active／archived | 對應 service | internal |

SA 要先回答：狀態能否回轉？使用者看到的是本系統狀態還是 upstream 狀態？哪些角色能改狀態？哪些欄位不能被使用者直接改？**business semantics 先定義，技術設計才有穩定輸入。**

---

## 4. SD：把業務能力轉成系統契約

SD 文件把業務能力轉成系統可實作的邊界與契約，分成四組：

| 面向 | 內容 |
|------|------|
| **邊界與介面** | service boundary、API resource、operation、request/response schema |
| **安全與錯誤** | authentication、authorization、permission、error model、retryability、idempotency |
| **資料與執行** | DB schema、constraint、index、query pattern、transaction、concurrency |
| **非功能需求** | performance、audit、observability、migration、rollback、compatibility |

### API contract 必須明確說明

- **scope 由 server-side authorization 決定**，不信任 client 傳入的 id。
- filter / 參數語意寫進 contract（例如 status、日期範圍如何過濾）。
- 大量資料用 **cursor pagination**，index 對應實際 query pattern。
- response schema、error code、retryability、timeout、rate limit 都要明確。
- API 提供穩定的 business capability interface，**不必逐一暴露畫面欄位**。
- 用 **OpenAPI** 固化 request/response/error schema，作為共同格式。

### Technical Data Dictionary（API 與 DB 的實際表示）

| Business field | API type | DB type | Nullable | Constraint | Index | Transform |
|----------------|----------|---------|----------|------------|-------|-----------|
| status | enum string | varchar(32) | No | status enum | composite | direct mapping |

SD 還需決定：primary key、foreign key、unique constraint、normalization/denormalization、transaction boundary、locking、concurrency、retention、migration。

> **業務資料字典定義語意，技術資料字典定義實作，兩者不混在一起。**

---

## 5. 後端 Implementation Contract 檢查清單

後端規格要描述「資料背後的查詢能力、來源、權限與效能」，不能只記錄「這有一個 API」。至少交代：

- **API 面**：operation、handler/use case mapping、request validation、business validation、response schema、field source。
- **安全面**：authorization、resource scope、error model、error forwarding、retry behavior、negative path。
- **資料面**：repository、upstream client、transaction boundary、query pattern、expected scale、index、latency target。
- **可觀測面**：audit event、correlation id、metrics、logs、traces。
- **測試面**：unit、integration、contract、permission、performance tests。

---

## 6. 用 Traceability 串起交付

同一份 feature spec 依角色輸出不同 view，但至少維護一張跨角色追蹤 mapping：

| 操作/需求 | Requirement | Business rule | API ID | Data effect | Verification ID |
|-----------|-------------|---------------|--------|-------------|-----------------|
| 建立 resource | REQ-001 | BR-001 | API-001 | insert resource | AC-001 |

> **如果一個操作沒有對應的 business rule、API 或 verification，代表規格還缺一段。** `requirement ID`、`API ID`、`Verification ID` 讓交付物與後續驗證可以互相追蹤。

---

## 7. Slice、驗證與回寫

### 7.1 切成可獨立 review 的 slice

不要一次產出巨大變更（Coinbase 教訓：單一 agent 一次生成 19 檔案、14,000 行，無法有效 review）。做法：

1. 先產出 **plan**（依 service / component 拆解），等待確認。
2. 再建立 **scaffold**：route、permission、migration、test skeleton、validation gates。
3. 最後由 worker 依 slice 實作，開出較小的 draft PR。

> 每個 slice 都有自己的 requirement IDs、API IDs、tests、owner 和 reviewer，審查才跟得上產出速度。

### 7.2 分層驗證

| 驗證層 | 主要問題 |
|--------|----------|
| Business | 功能是否符合 use case 和 business rule？ |
| API / Data | contract、schema、query 和 error behavior 是否正確？ |
| Security / Operations | 權限、audit、效能、監控、rollback 是否可用？ |

### 7.3 變更回寫到正確層次

實作過程出現變更時，回寫到正確層次，**不要只在 code 裡改**：

| 變更類型 | 回寫位置 |
|----------|----------|
| 技術細節改變 | Technical Design |
| API 改變 | API contract（OpenAPI / specs） |
| business semantics 改變 | Business Requirements（回到 SA 決策） |

---

## 8. 對應本 repo 的既有流程

| 本文概念 | 本 repo 對應物 |
|----------|----------------|
| Evidence Pack | 需求盤點（`docs/specs/requirements.md` 的來源分析） |
| SA 業務需求 | `docs/specs/<feature>/README.md` 的 Requirements + Acceptance Criteria |
| SD 技術設計 | `docs/specs/<feature>/` 的 API contract + data model |
| 架構決策 | `docs/adr/`（ADR-001 ~ 009） |
| API contract | `docs/api/` OpenAPI 3.0 |
| DB design | `docs/db/`（DDL + DML + ER） |
| Verification ID | Acceptance Criteria（GIVEN/WHEN/THEN）|

## 9. 核心原則摘要

1. **先 evidence，再規格**：把 observed / inferred / unknown 分開，推測不成為決策。
2. **角色分層**：SA 定語意 → SD 定契約 → 後端實作 → QA 驗證，各層不越界。
3. **契約可追蹤**：每個操作都能回指 requirement、business rule、API、data effect、verification。
4. **切小 slice**：小 PR、獨立 review、每段可驗證。
5. **變更回寫**：改對層次，不只在 code 裡改。
6. **AI 加速翻譯，人負責決策**：business meaning、technical contract、verification conditions 由人把關。

---

## 10. 動筆前檢查清單（寫前對照天條）

> 本節是**實戰教訓的回寫**：過去 agent 曾三次違反此紀律——擅自增補天條沒有的功能（保底、即時動態機率、活動暫停態、個人記錄查詢）、把技術實作（Redis/Lua/SQL/JWT/BCrypt）抄進 SA 層 FR 條文、先寫再改浪費 review 成本。**方法論寫在文件裡，不代表 agent 會自動遵守**，因此把「動筆前檢查」固化成強制步驟。

任何 FR / SA / SD 內容**落筆前**，先逐條對照原始需求原文（本 repo 的天條見 `docs/specs/requirements.md` §0），回答三問：

| 檢查 | 問題 | 違反時 |
|------|------|--------|
| **增補檢查** | 這個功能／欄位／狀態，原文有沒有明列？ | 未明列 → **不做**，除非使用者明確指示 |
| **分層檢查** | 這個詞是「業務語意」還是「技術實作」？ | 技術實作（Redis、SQL、演算法、函式庫）→ 留在 ADR / SD 層，不寫進 SA 的 FR |
| **牴觸檢查** | 我的描述是否削弱或改寫了原文的約束？ | 牴觸 → 以原文為準，回頭改規格，不在 code/文件裡硬拗 |

> **最小需求**：目標是「系統的完整性與機制健全」，不是堆疊功能。寧可小而有料，不可大而空泛。**快速開發**：先做天條明列的，其餘留待使用者指示。

---

## 11. 實作契約索引 (Implementation Contract Index)

> 契約落地為實作的**中間層**：每支 API 有一份「實作方式」文件，加上每服務 API 總表與全系統錯誤碼總表，作為 Backend 實作與 QA 驗證的索引。**任何 agent 動筆實作前，先查這裡。**

### 11.1 檔案位置

| 交付物 | 位置 | 命名 |
|--------|------|------|
| API 實作方式（每支 API 一份） | `docs/api/impl/<api-id>.md` | 以 **API ID** 為檔名（如 `campaign-draws-001.md`） |
| 每服務 API List 總表 | `docs/api/api-list/<service>.md` | `auth-service.md` / `campaign-service.md` / `inventory-service.md` / `gateway-service.md` |
| 錯誤碼總表（全系統唯一） | `docs/api/error-list.md` | 固定單一檔 |
| 實作方式模板 | `docs/templates/api-template.md` | 固定單一檔 |

### 11.2 實作方式模板（每支 API 必備）

依 `docs/templates/api-template.md`：`API-ID` + `API-NAME` → 描述大意 → 流程圖（mermaid 循序圖）→ 邏輯（逐步驟 + 例外處理 + 錯誤碼）→ 錯誤代碼清單（table）。

### 11.3 紀律

- **錯誤碼只從 `docs/api/error-list.md` 引用**，不得在各 API 文件自造語意重複的 code。
- **API ID 是唯一鍵**：OpenAPI `operationId`、`docs/api/impl/` 檔名、`docs/api/api-list/` 索引、追溯矩陣，四處必須一致。
- 實作中若發現 API 契約需變更 → 先回寫 OpenAPI + 本索引，再改 code（§7.3）。

---

## 12. 測試與程式碼生成紀律 (Testing & Code Generation)

> 實作階段（寫 code）前必讀。這些決策把「文件 vs 實作漂移」交給工具強制，把「測試保護什麼」交給不變量判斷。

### 12.0 開發方式速覽（新 session 接手先讀這張表）

> 全專案一條主線：**每一層都有一個「真相來源」＋一個「工具強制一致」**，不靠人記得。

| 層 | 真相來源 | 工具強制一致 |
|----|----------|--------------|
| 需求／情境 | 天條 §0 → story → SA | 追溯矩陣（story→FR→UC→AC→API） |
| route / schema | OpenAPI YAML（`docs/api/openapi/`） | openapi-generator → `contracts` module |
| DB schema | `docs/db/*.md` DDL | Flyway migration + `ddl-auto=validate` |
| entity ↔ DTO | entity 欄位 | MapStruct + `unmappedTargetPolicy=ERROR` |
| 業務不變量 | SA 的 business rule / AC | TDD unit test |

- **動筆實作前，依序查**：① `docs/api/impl/<api-id>.md`（實作方式）→ ② `docs/stories/`（情境）→ ③ `docs/specs/<service>/`（UC/AC）→ ④ 對應 ADR。
- **改任何真相來源，先回寫文件再改 code**（§7.3）：YAML / DDL / SA 變更都比 code 先行。

### 12.1 程式碼生成（ADR-011）

| 面向 | 工具 | 方式 |
|------|------|------|
| API → code | openapi-generator | 集中於 **`contracts` module**，生成 **API interface + DTO**（auth/campaign 的 REST + inventory 的事件 payload）；controller 手寫 `implements`。gateway 不走 codegen。生成物入版控、勿手改 |
| DB schema → ORM | Flyway + JPA | `docs/db/*.md` 的 DDL 直接落成 Flyway migration（DDL 是真相）；JPA entity 手寫（放 `model.entity`）、repository 獨立 package，`ddl-auto=validate` 啟動校驗一致 |
| dev datasource | H2 `MODE=PostgreSQL` | 讓 migration 共用一份 PostgreSQL 方言 DDL；DataSource 依 env 切換（NFR-04）。**防超抽併發正確性仍以 PostgreSQL（Testcontainers）驗證** |

- **package 慣例**：entity → `model.entity`；DTO 來自 `contracts`（generated）；手寫傳輸 DTO（若有）→ `model.dto`；value object → `model.vo`；repository → `repository`；mapper → `mapper`。
- 生成物（`contracts/src/generated/`）**入版控、勿手改**；YAML 變更 → 重跑 generate。
- 詳細理由與 Alternatives 見 [ADR-011](docs/adr/011-code-generation-persistence-strategy.md)。

### 12.2 物件映射與 boilerplate（ADR-012）

| 面向 | 決策 | 理由 |
|------|------|------|
| entity ↔ DTO | **MapStruct** + `unmappedTargetPolicy=ERROR` | 編譯期欄位遺漏檢查（非為省 setter，效能與手寫相同） |
| Lombok | **不用** | generated DTO 已自帶 accessor；`@Data`/`@Builder` 在 JPA entity 有 lazy-loading / 雙向關聯陷阱；隱式 code 是 review 負擔 |
| 純資料載體 | **Java `record`** | immutable、getter 自動；JPA entity 不用 record（需可變 + 代理） |

- 詳細理由見 [ADR-012](docs/adr/012-object-mapping-boilerplate.md)。

### 12.3 單元測試（TDD）

完整規約見 [`docs/rules/unit-testing.md`](docs/rules/unit-testing.md)。核心四條：

1. **unit = 單一行為單元，不是方法**；測試保護**不變量**，不是 coverage。
2. **寫前先問**：「這條規則改錯，哪個業務受害？」受害的那個，才值得測。不變量從 SA 的 business rule / AC 擷取。
3. **mock 預設 classical**：Stub 給固定答案 + state verification；少用 behavior verification（鎖實作）。需 mock DB/HTTP/MQ 才能測 → 先重構分離，不是補 mock。
4. **AI 生成測試三坑**（審查用）：鏡射實作、重述錯誤邏輯、全 happy path。以三問過篩（換一種正確寫法還過嗎？需求誤解抓得到嗎？唯一會紅的理由是「有人改實作」嗎？）。
5. **TDD 節奏**：Red→Green→Refactor，小步快跑；compile suite 秒級、commit suite ≤10 分鐘。

---

## 13. Rules 索引 (Rules Index)

> 編程規約的**唯一來源**（`docs/rules/`）。動筆寫 code 前依需查閱；與 code 衝突時以規則為準（§7.3 回寫）。

| Rule | 檔案 | 定位 |
|------|------|------|
| Naming | [`docs/rules/naming.md`](docs/rules/naming.md) | 分層命名；**JPA entity 一律 `XxxEntity` 後綴**（dirty-checking 警示）；generated enum 例外不加後綴 |
| Exceptions | [`docs/rules/exceptions.md`](docs/rules/exceptions.md) | 業務異常 `ApiException extends RuntimeException` 不宣告 throws；`@RestControllerAdvice` 兜底轉 envelope |
| Logging | [`docs/rules/logging.md`](docs/rules/logging.md) | SLF4J + Logback；佔位符 `{}`；敏感資訊不落 log |
| Error Codes | [`docs/rules/error-codes.md`](docs/rules/error-codes.md) | 分段規約；**唯一碼表在 [`docs/api/error-list.md`](docs/api/error-list.md)** |
| Unit Testing | [`docs/rules/unit-testing.md`](docs/rules/unit-testing.md) | 保護不變量、classical-first mock、AI 三坑 |
| OpenAPI Contract | [`docs/rules/openapi-contract.md`](docs/rules/openapi-contract.md) | API ID / operationId / Model 命名 |
| REST API | [`docs/rules/rest-api.md`](docs/rules/rest-api.md) | REST 命名/方法/envelope；POC 不分頁 |
