# 錯誤碼總表 (Error Code List)

> 全系統**唯一錯誤碼來源**（數字碼），用於 response envelope 的 `code` 欄位。分段規約見 [`docs/rules/error-codes.md`](../rules/error-codes.md)，HTTP status 對映依《RESTful API 設計指南》§6.2。
> 各 API 實作檔案（`docs/api/impl/<api-id>.md`）只引用本表已定義的 code，**不得自行新增語意重複的 code**。

| 錯誤碼 | HTTP | 語意 | 來源服務 | 對應 API |
|--------|------|------|----------|----------|
| `00000` | 200 | 成功 | 全部 | — |
| `A0000` | 400 | 用戶端錯誤（一級，結構性輸入錯誤等通用碼） | 全部 | — |
| `A0100` | 400/409 | 註冊錯誤（二級） | auth | auth-users-001 |
| `A0101` | 409 | username 已存在 | auth | auth-users-001 |
| `A0102` | 409 | email 已存在 | auth | auth-users-001 |
| `A0103` | 400 | 註冊輸入驗證失敗（缺欄位／email 格式／密碼空） | auth | auth-users-001 |
| `A0200` | 401 | 登入異常（二級，通用碼） | auth | auth-tokens-001, auth-tokens-002 |
| `A0201` | 401 | 帳號或密碼錯誤（**與帳號不存在同碼，不洩漏存在性**） | auth | auth-tokens-001 |
| `A0202` | 401 | 憑證過期 | auth / gateway | 全部受保護 API |
| `A0203` | 401 | 憑證無效（簽章/格式）或缺憑證 | auth / gateway | 全部受保護 API |
| `A0300` | 4xx | campaign 錯誤（二級，通用碼） | campaign | campaign-* |
| `A0301` | 404 | 活動不存在，或非 ACTIVE 之抽獎請求 | campaign | campaign-campaigns-002/004/005, campaign-prizes-001, campaign-draws-001 |
| `A0302` | 409 | 活動狀態衝突（非法轉移／非可編輯狀態） | campaign | campaign-campaigns-004/005, campaign-prizes-001 |
| `A0303` | 422 | 獎品機率總和 ≠ 100% | campaign | campaign-prizes-001 |
| `A0304` | 422 | 獎品機率越界 `[0,100]` | campaign | campaign-prizes-001 |
| `A0305` | 422 | 缺 `THANK_YOU` 獎品 | campaign | campaign-prizes-001 |
| `A0306` | 429 | 個人抽獎次數超限（活動期間總額） | campaign | campaign-draws-001 |
| `A0307` | 409 | 冪等鍵衝突（併發重入） | campaign | campaign-draws-001 |
| `A0400` | 403 | 權限不足（越權存取管理功能） | 各服務 | campaign-campaigns-003/004/005, campaign-prizes-001 |
| `A0500` | 429 | 請求過於頻繁（gateway 限流，per-user/per-IP） | gateway | 全部對外 API |
| `A0501` | 400 | 抽獎請求缺 `Idempotency-Key` header | gateway | campaign-draws-001 |
| `B0000` | 500 | 系統錯誤（一級） | 全部 | — |
| `C0000` | 502/504 | 呼叫第三方/下游錯誤 | 全部 | — |

## 備註

- `A0303/A0304/A0305` 為「結構合法但違反業務不變量」的**語意驗證**，回 `422`；純結構性輸入錯誤回 `400`（`A0000`）。
- `A0306`（campaign 抽獎次數超限）與 `A0500`（gateway 限流）為**不同維度**：前者是業務規則（活動期間總額），後者是邊界保護（請求頻率），不得混用。
- 抽獎 replay（ADR-005）：同複合冪等鍵重送成功回 `200` + 原 body，**不**回任何錯誤碼。
