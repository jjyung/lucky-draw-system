# OpenAPI Contract Rules

## API ID (MUST)

Each API MUST have a unique API ID.

Format:

```
{service-name}-{resource-name-plural}-{3-digit-seq}

```

Rules:

- service-name: lowercase kebab-case

- resource-name: plural

- sequence: 001-999

- must be unique within same service-resource group

Example:

- library-books-001

- member-users-013

Usage:

- API ID MUST appear in API name

- API ID MUST NOT be used as operationId

- **Source of truth**：完整 API ID 清單由 **SD 階段**定義於 `docs/api/`（API 清單文件）；OpenAPI YAML 中每個 operation 的 API ID **必須與清單一致**，清單即 YAML 的索引。

---

## operationId (MUST)

- operationId MUST use path（依路徑推導），格式為 camelCase `{verb}{Resource}{action?}`，例如：`getCampaigns`、`getCampaignById`、`postCampaignDraw`、`patchCampaignStatus`

- MUST NOT use API ID as operationId

- operationId 與 API ID 的對應以 **SD 的 API 清單**為準，兩者須一致

---

## Model Naming (MUST)

All models MUST distinguish Request and Response.

Format:

```
{HttpMethod}{Resource}RequestDTO
{HttpMethod}{Resource}ResponseDTO

```

- `Resource` 依「該 API 實際操作的資源」命名，**單複數視使用狀況**：集合／列表用複數（`GetCampaignsResponseDTO`）、單一資源用單數（`GetCampaignResponseDTO`）、action 端點以動作命名（`PostCampaignDrawRequestDTO`）。

HttpMethod:

- Get

- Post

- Put

- Patch

- Delete

Examples:

- GetBooksRequestDTO

- PostBooksResponseDTO

- PatchUsersRequestDTO

---

## Naming Conflict (SHOULD)

When conflict occurs, add qualifier:

- Admin

- Internal

- Public

- Summary

- Detail

- V2

Example:

- GetBooksDetailResponseDTO

- PostAdminBooksRequestDTO