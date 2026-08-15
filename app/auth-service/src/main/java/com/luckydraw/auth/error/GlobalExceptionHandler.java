package com.luckydraw.auth.error;

import com.luckydraw.auth.api.model.ErrorEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 統一錯誤回應（envelope：{ code, message, data:null }）。
 * HTTP status 與 code 的對映依 error-list.md / api README §2.3。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorEnvelope> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorEnvelope().code(ex.getCode()).message(ex.getMessage()).data(null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorEnvelope()
                        .code(ErrorCodes.STRUCTURAL_INVALID)
                        .message("輸入驗證失敗")
                        .data(null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorEnvelope> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorEnvelope()
                        .code(ErrorCodes.STRUCTURAL_INVALID)
                        .message("輸入驗證失敗")
                        .data(null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorEnvelope()
                        .code(ErrorCodes.SYSTEM_ERROR)
                        .message("系統錯誤")
                        .data(null));
    }
}
