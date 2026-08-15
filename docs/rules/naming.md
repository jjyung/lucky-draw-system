# Naming（命名風格）

> 本專案命名慣例。基礎規範（類 UpperCamelCase、方法 lowerCamelCase、常量全大寫、不用拼音/中文）遵循《阿里巴巴 Java 開發手冊》，此處只列**本專案特定的分層命名**。

## 1. 分層命名（本專案核心）

| 層 | 後綴／命名 | 範例 | 說明 |
|----|-----------|------|------|
| JPA entity | **`XxxEntity`** | `UserEntity` / `CampaignEntity` / `PrizeEntity` | 見 §2「entity 後綴」 |
| Repository | `XxxRepository` | `UserRepository` | Spring Data JPA |
| Service | `XxxService` | `AuthService` / `PrizeService` | 業務邏輯 |
| Mapper | `XxxMapper` | `UserMapper` / `CampaignMapper` | MapStruct（ADR-012） |
| Controller | `XxxController` | `AuthController` | implements generated Api |
| 手寫傳輸 DTO | `XxxDTO` | — | generated DTO 在 contracts，手寫很少 |
| value object | `XxxVo` | — | 純資料載體可改用 `record` |
| 例外 | `XxxException` | `ApiException` | |

## 2. Entity 後綴（MUST）

JPA entity **一律加 `Entity` 後綴**（如 `UserEntity`），理由：

- **警示意義**：entity 是 JPA 持久化代理，在 persistence context 存活期間，任何 setter 改動 flush 時會**自動回寫 DB**（dirty checking），不需明確 save()。`Entity` 後綴標記「這是持久化代理，別當普通資料亂改」。
- **與 generated DTO 區分**：openapi-generator 產的 DTO（`UserResourceDTO` 等）無此後綴，命名上即分辨「持久化層」vs「契約層」。

> 註：後綴是**紀律標記**，不是安全機制。真正的防護是：① `spring.jpa.open-in-view: false`（已設）；② entity 永不直接回傳 controller（用 MapStruct 轉 DTO）。三者疊加。

## 3. Enum 後綴

- **所有 enum 一律加 `Enum` 後綴**（如 `CampaignStatusEnum`、`PrizeTypeEnum`），含 openapi-generator 生成的 enum。
- **generated enum 的後綴在 YAML 層處理**：OpenAPI 的 enum schema 命名為 `XxxEnum`（如 `CampaignStatusEnum`），generator 即生成同名 enum——**改 YAML 再重生成，而非改生成物**。內嵌 enum（如 DTO 內欄位）generator 會自動加 `Enum` 後綴（如 `RolesEnum`）。
- 理由：enum 是「型別」，與 DTO（資料載體）在使用語意上不同，命名上區分（`Enum` vs `DTO`）有助於識別。

## 4. 例外命名

- 業務例外 `ApiException extends RuntimeException`，不宣告 `throws`（見 [exceptions.md](exceptions.md)）。

## ref

- 阿里巴巴 Java 開發手冊（基礎命名規範）
