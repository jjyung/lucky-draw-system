# Exceptions（異常處理）

## 1. 業務異常

- 業務異常用自訂 `ApiException extends RuntimeException`，承載 **錯誤碼（`error-list.md`）** 與 **HTTP status**。
- **不宣告 `throws`**（runtime exception，Spring 慣例），由 `@RestControllerAdvice` 統一轉成 envelope 回應。

## 2. 系統異常

- 未預期的系統異常（DB/Redis/MQ 等）由 `@RestControllerAdvice` 的**兜底 handler** 捕獲，轉成 `B0000` / 500。
- 兜底 `@ExceptionHandler(Exception.class)` 是**允許且必要的例外**——它不「吞掉」異常，而是轉成統一 envelope 並記錄 log。

## 3. 轉換原則

- 捕獲底層異常需重新拋出時，保留原始異常作 cause：`throw new ApiException(..., e)`。
- 記錄完整堆疊：`log.error("msg", e)`。

## 4. 敏感資訊

- 異常訊息與 log **不得**包含密碼、憑證、內部系統資訊。
- 登入失敗「帳號不存在」與「密碼錯誤」回**同一錯誤碼**，不洩漏帳號存在性（AC-AUTH-006）。
