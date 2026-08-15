# Error Codes（錯誤碼）

> ⚠️ **唯一來源已移至 [`docs/api/error-list.md`](../api/error-list.md)**。本檔僅保留分段規約與指引，不再重複碼表。

## 分段規約

| 段 | 語意 | HTTP |
|----|------|------|
| `00000` | 成功 | 200 |
| `Axxxx` | 用戶端錯誤 | 400/401/403/404/409/422/429 |
| `Bxxxx` | 系統錯誤 | 500 |
| `Cxxxx` | 呼叫第三方錯誤 | 502/504 |

## 使用規約

- response envelope 的 `code` 欄位（見 [rest-api.md](rest-api.md) §7.2）。
- 成功一律 `00000`；錯誤碼**只從 `error-list.md` 引用**，不得自造語意重複的 code（AGENTS.md §11.3）。
- 逐碼的 HTTP 對映以 `error-list.md` 為準。
