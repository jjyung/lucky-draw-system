package com.luckydraw.auth.error;

import org.springframework.http.HttpStatus;

/**
 * 錯誤碼常數（全系統唯一來源 docs/api/error-list.md）。
 * 各 API 只引用此處與 error-list.md 已定義的 code，不自行新增。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String OK = "00000";

    // 註冊（auth-users-001）
    public static final String USERNAME_EXISTS = "A0101";
    public static final String EMAIL_EXISTS = "A0102";
    public static final String REGISTER_INVALID = "A0103";

    // 登入／憑證（auth-tokens-001/002）
    public static final String BAD_CREDENTIALS = "A0201";
    public static final String TOKEN_EXPIRED = "A0202";
    public static final String TOKEN_INVALID = "A0203";

    // 通用
    public static final String STRUCTURAL_INVALID = "A0000";
    public static final String SYSTEM_ERROR = "B0000";

    // 常用錯誤
    public static ApiException usernameExists() {
        return new ApiException(USERNAME_EXISTS, HttpStatus.CONFLICT, "username 已存在");
    }

    public static ApiException emailExists() {
        return new ApiException(EMAIL_EXISTS, HttpStatus.CONFLICT, "email 已存在");
    }

    public static ApiException badCredentials() {
        return new ApiException(BAD_CREDENTIALS, HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
    }

    public static ApiException tokenExpired() {
        return new ApiException(TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED, "憑證過期");
    }

    public static ApiException tokenInvalid() {
        return new ApiException(TOKEN_INVALID, HttpStatus.UNAUTHORIZED, "憑證無效");
    }
}
