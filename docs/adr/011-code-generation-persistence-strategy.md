# ADR-011: 程式碼生成與持久層策略 (Code Generation & Persistence Strategy)

**Date:** 2026-08-15
**Status:** Accepted

## Context

進入實作階段前，需決定兩件影響所有後續 code 骨架的事：

1. **API 契約 → 程式碼**：既有 `docs/api/openapi/*.yaml` 是 route/schema 的真相來源（API ID、operationId、request/response DTO）。若手寫 controller/DTO，會再次出現「文件與實作漂移」——正是本 repo 反覆修掉的問題。因此希望「openapi → code」由工具生成，確保一致性與穩定性。

2. **DB schema → ORM**：既有 `docs/db/*.md` 是手寫的 PostgreSQL DDL（含 `BIGSERIAL`、`TIMESTAMPTZ`、`JSONB`、`NUMERIC(5,2)`、`CHECK`、複合 `UNIQUE`、`COMMENT ON`）。同樣希望 schema 與 ORM 之間由工具保證一致，而非靠人工對齊。

同時，dev 環境（ADR-002）目前為「SQLite / H2 二選一」，兩者對 PostgreSQL 專屬型別（`JSONB`、`TIMESTAMPTZ`）與 `CHECK` 的支援不一，會影響 migration 能否共用一份 DDL。

## Decision

### 1. OpenAPI → code：openapi-generator，只生成 DTO + API interface

- 採 **openapi-generator**（`openapi-generator-gradle-plugin`），模式為**生成 API interface + request/response DTO**，controller **手寫**並 `implements` 生成的 interface。
- **適用範圍**：僅 **auth-service** 與 **campaign-service** 兩個「有 REST endpoints」的服務。**gateway-service**（對外 surface 彙整、路由表）與 **inventory-service**（事件契約、無 REST paths）**不**走 codegen。
- **OpenAPI YAML 是唯一 route/schema 真相**：`operationId`、path、request/response DTO 由 generator 保證與 YAML 一致；controller 只寫業務轉發，不重複定義 route/schema。這與 AGENTS.md §11.3「API ID 是唯一鍵、OpenAPI `operationId` 與 `docs/api/impl/` 檔名一致」的紀律互補。
- codegen 綁進 build：`generateApi` task 於每次 build 重新產出，YAML 變更自動反映。生成物放 `build/generated`，不入版控。

### 2. DB schema → ORM：Flyway（DDL 為 migration 真相）+ JPA entity + validate

- **Flyway** 管理 schema：`docs/db/*.md` 的 DDL 直接落成 Flyway migration（`V1__init_<service>.sql`），**DDL 是唯一真相**。每服務一套 migration（對齊 ADR-002 三 schema）。
- **JPA entity 手寫**，並以 **`hibernate.ddl-auto=validate`** 在啟動時校驗 entity ↔ DB schema 一致，mismatch 即 fail fast——以此把「手寫 entity 漂移」在啟動時抓出，作為工具化保證一致的手段。
- **不採反向工程**（hbm2java / jOOQ codegen）：反向工程對 `CHECK`、`COMMENT ON`、複合 `UNIQUE`、`JSONB` 等 PostgreSQL 細節還原度不足，會丟失 `docs/db` 已 review 過的語意。

### 3. dev 環境收斂為 H2 + `MODE=PostgreSQL`

- dev profile 統一採 **H2 + `MODE=PostgreSQL`**，讓 migration 共用一份 PostgreSQL 方言 DDL（`BIGSERIAL`/`JSONB`/`TIMESTAMPTZ`/`CHECK` 於 PG mode 下可跑）。
- **棄 SQLite** 作為 migration 目標：SQLite 對 `JSONB`、`TIMESTAMPTZ`、`CHECK` 支援不足，無法承載同一份 DDL。
- 仍滿足 **NFR-04**（DataSource 可依環境變數切換）：`application-{dev,prod}.yml` + Spring Profiles 切換 datasource URL / driver / 連線池參數，多環境部署不受影響。
- **防超抽正確性測試**（`FR-INV-02`、ADR-002 後果）仍以 **PostgreSQL（Testcontainers）** 驗證，H2 僅供地端開發流程，不作防超抽證據。

## Consequences

**正面：**

- **文件與實作零漂移**：route/schema 由 openapi-generator 保證、DB schema 由 Flyway migration 保證、entity 由 validate 保證，三處「真相」都由工具強制一致。
- **DDL 零轉譯**：`docs/db` 的 DDL 直接即 migration，不需轉成 changelog XML。
- **可平行開發**：contract（OpenAPI + DDL）定案後，frontend/backend、各 service 可依契約並行。

**負面 / 需付出的代價：**

- **codegen 產物需被視為「生成、勿手改」**：controller 手寫、DTO/interface 生成，兩者邊界要紀律化（生成檔放 `build/generated`，不 commit，避免手改生成物）。
- **H2 非 PostgreSQL 完全等價**：`WHERE stock > 0` 條件更新的並發語意與 row lock 行為在 H2 與 PostgreSQL 不同（ADR-002 後果），H2 只做功能驗證，併發正確性仍須 PostgreSQL。
- **validate 的侷限**：`hibernate.ddl-auto=validate` 能抓「entity 與 schema 型別/欄位 mismatch」，但抓不到「業務語意漂移」（如 CHECK 值域），後者靠 unit test 釘住（見 `docs/rules/單元測試.md`）。

## Alternatives

- **openapi-generator 全量生成 server stub**：一致性最強，但 generator 產的 controller 常需 `.openapi-generator-ignore` 保護手寫區，且產物品質參差；採「interface + DTO」已兼顧一致與可控。
- **jOOQ / Hibernate Reverse Engineering 反向生成 entity**：見 §Decision 2，對 PostgreSQL 細節還原度不足，否決。
- **Liquibase（changelog）**：rollback 與跨 DB 條件較強，但需把 DDL 轉成 changelog XML（多一層轉譯，`CHECK`/`COMMENT`/`JSONB` 表達繁瑣）；本專案無「回滾 prod schema」強需求、dev 以 H2 PG-mode 收斂方言差異，Liquibase 的優勢用不到，卻要付轉譯成本，否決。詳見本文 §Context。
- **Entity-first（`hibernate.ddl-auto=update/create` 生成 schema）**：與「`docs/db` DDL 是真相」的方向相反，且 Hibernate 生成的 DDL 不易表達 CHECK/COMMENT/index 細節，否決。
