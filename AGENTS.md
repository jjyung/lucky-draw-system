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
