# ADR-012: 物件映射與 Boilerplate 策略 (Object Mapping & Boilerplate)

**Date:** 2026-08-15
**Status:** Accepted

## Context

進入 Slice 1（auth-service）實作後，需要決定兩件影響所有 service 寫法的事：

1. **entity ↔ DTO 映射**：實作中出現 `User → UserResourceDTO` 等映射。過去慣用手寫 setter 或 MapStruct。在 AI agent 代勞寫 boilerplate 的開發方式下，「省 setter」不再是選型理由，真正的決策維度是**維護性與正確性保證**。
2. **getter/setter 與 boilerplate**：Lombok 常用於省去 getter/setter/建構子。但在 agent 代勞、且 DTO 已是 openapi-generator 生成（自帶 getter/setter）的前提下，Lombok 是否仍有價值需重新評估。

## Decision

### 1. entity ↔ DTO 映射：MapStruct + `unmappedTargetPolicy = ERROR`

- 採 **MapStruct**（編譯期 annotation processor，生成 setter 呼叫，**效能等同手寫**，無 runtime reflection）。
- **核心理由不是「省 boilerplate」，而是「編譯期欄位遺漏檢查」**：`unmappedTargetPolicy = ERROR` 讓「entity 加欄位但 DTO 映射漏了」在**編譯期報錯**，而非靜默遺漏。這與本 repo 一貫的「工具強制一致」方法論一致——openapi-generator 鎖 route/schema、Flyway 鎖 DDL、`validate` 鎖 entity↔DB，MapStruct 鎖 entity↔DTO。
- 手寫 setter 無此保證：agent 改 entity 時常忘了同步 mapping，欄位靜默丟失，測試不一定抓得到。
- Mapper 介面放 `mapper` package，`componentModel = "spring"`，標 `@Mapper`；欄位名一致時自動映射，欄位轉換（如 `Role.code → RolesEnum`）以 `default` method 顯式表達。

### 2. Lombok：不使用（生成 DTO 已自帶 getter/setter）

- **generated DTO（`contracts`）**：openapi-generator 已生成 getter/setter，**不能也不該**套 Lombok。
- **JPA entity**：agent 手寫 getter/setter（顯式、可讀）。Lombok 的 `@Data`/`@EqualsAndHashCode`/`@ToString` 在 JPA entity 上會踩 **lazy loading + 雙向關聯**（如 `@ManyToMany` Role↔User）的遞迴坑；`@Builder` 與 JPA 無參數建構子交互易錯。故**不使用 Lombok**。
- **純資料載體**（value object、event 內部暫存、回傳聚合）：採 **Java `record`**（immutable、getter 自動、equals/hashCode 內建），JPA entity 不用 record（需可變 + 代理）。

### 3. 效能聲明（非決策維度）

MapStruct / 手寫 setter / Lombok 三者皆為**編譯期生成或直接手寫**，無 runtime reflection，效能等同。**效能不是選型理由**，決策維度是「編譯期正確性保證」與「顯式可讀性」。

## Consequences

**正面：**

- **欄位遺漏在編譯期被抓**：`unmappedTargetPolicy=ERROR` 提供安全網，降低 agent 重構時的靜默遺漏。
- **code 顯式可讀**：無 Lombok 隱式 getter/setter，agent 與 reviewer 不需「想像」生成的方法。
- **零額外 runtime 成本**：MapStruct 生成的是 setter 呼叫。

**負面 / 需付出的代價：**

- **多一個 annotation processor 依賴**（`mapstruct` + `mapstruct-processor`）。
- **複雜映射的可讀性**：巢狀/條件映射用 MapStruct 的 expression 可能難讀；此類場景少，遇複雜映射可退回手寫 method。
- **entity 的 getter/setter 較冗長**：由 agent 代勞，實際成本低；以「顯式」換取，屬刻意取捨。

## Alternatives

- **手寫 setter**：無依賴、可讀，但缺乏欄位遺漏檢查，agent 改 entity 易漏同步，否決（除非映射極少且穩定）。
- **Lombok `@Getter`/`@Setter`（僅這兩個）**：可接受但收益低（agent 寫 boilerplate 零成本），且引入隱式性；為維持「顯式優先」一致原則，不採用。`@Data`/`@Builder` 因 JPA 陷阱明確否決。
- **ModelMapper / Dozer（runtime reflection）**：效能差、型別不安全、無編譯期檢查，否決。
- **手寫 DTO 轉換 util 類**：與手寫 setter 同問題（無編譯期檢查），且多一層間接，否決。
