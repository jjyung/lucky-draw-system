# ADR-001: Monorepo 多模組建置結構 (Monorepo Multi-Module + Gradle)

**Date:** 2026-08-13
**Status:** Accepted

## Context

lucky-draw-system 為全新（greenfield）的分散式電商抽獎平台，由 4 個微服務組成：`api-gateway`、`auth-service`、`campaign-service`、`inventory-service`。服務之間需要共用 DTO、Exception 與 Utils，但目前 repo 沒有任何程式碼。

需要決定的關鍵點：

1. **單一 repo vs 多個 repo** — 影響跨服務變更（cross-service change）的發布成本，與 CI/CD 的複雜度。
2. **建置工具** — Maven vs Gradle，影響 build 速度、設定可讀性與生態支援。
3. **共用程式碼的歸屬** — 直接複製貼上 vs 抽取共用 module。

考量到本專案為 POC 起家但預留 prod 彈性，且團隊規模小，**跨服務原子性變更（atomic commit）與低協作開銷**比「獨立部署彈性」更優先。

## Decision

採用 **Monorepo + Gradle 8.x 多模組建置**，並使用 **Gradle Wrapper** 鎖定版本，確保任何開發者/CI 環境建置結果一致。

目錄結構如下：

```text
lucky-draw-system/
├── settings.gradle          # 模組宣告 (include ...)
├── build.gradle             # root 共用 plugin 與 dependencyManagement
├── gradle/wrapper/          # Gradle Wrapper (gradlew / gradlew.bat)
├── common/                  # 共用 library module（非 Spring Boot 應用程式）
│   └── src/main/java/...
├── services/
│   ├── api-gateway/         # Spring Boot 3 application module
│   ├── auth-service/        # Spring Boot 3 application module
│   ├── campaign-service/    # Spring Boot 3 application module
│   └── inventory-service/   # Spring Boot 3 application module
├── docker/                  # docker-compose 與建置檔
└── docs/                    # ADR 與架構文件
```

規則：

- **`common/` 是 library module**：包含共用 DTO、Exception（例如 `DrawException`、`ErrorCode`）、Utils（例如 Idempotency、Random 工具），**不包含** Spring Boot 應用程式，也不可直接啟動。
- **每個 service 是獨立的 Spring Boot 3 application module**：各自有自己的 `Application` 類別、`application.yml` 與依賴，透過 `implementation project(':common')` 引用共用碼。
- **Gradle 8.x + Wrapper**：root `build.gradle` 統一管理 `io.spring.dependency-management` 與 Spring Boot plugin 版本，避免各模組版本漂移。
- 每個 service 均可獨立 `./gradlew :services:campaign-service:bootRun` 或 `./gradlew :services:campaign-service:bootJar`，維持獨立部署能力。

## Consequences

**正面：**

- 跨服務共用碼（DTO / Exception）改動時，一次 commit 即可同步，搭配 CI 一起 build，避免「改了 common 卻忘了升級消費者」的版本錯位問題。
- Gradle Wrapper 保證 CI 與本機 build 版本一致；Gradle 的增量建置（incremental build）與 build cache 在大型 multi-module 下優於 Maven。
- 單一 repo 讓 IDE（IntelliJ IDEA）直接導入整份專案，跨服務 navigation 與 refactor 成本低。

**負面 / 需付出的代價：**

- 單一 repo 需要養成 **CI 上所有模組一起 build** 的紀律，否則壞掉的 service 會卡住其他人的 merge。
- 未來若某個 service 需要獨立對外開源或獨立成隊開發，需要再做 repo 拆分（repo split），這是已知的成本。
- Gradle 的學習曲線略高於 Maven（尤其 Groovy/Kotlin DSL 差異），需在 README 中補充指令集。

## Alternatives

- **Maven multi-module**：功能等價且生態成熟，但 Gradle 的 build 速度、`settings.gradle` 的彈性與 Spring Boot plugin 支援更適合本專案；最終選擇 Gradle 8.x。
- **Multi-repo（每 service 一 repo）**：獨立部署彈性最佳，但 POC 階段協作成本高、跨服務改動需多個 PR 同步，且共用碼需額外發布流程（如內部 Artifactory），過度工程。
- **單一 Spring Boot 應用（monolith-first）**：開發最快，但會直接抵銷本專案「分散式風控、高併發、服務獨立擴展」的核心目標，否決。
- **共用碼直接複製**：短期最快，但會導致 DTO/Exception 四處漂移，違反 DRY，長期維護成本最高，否決。
