package com.luckydraw.campaign.error;

import org.springframework.http.HttpStatus;

/**
 * 錯誤碼常數（全系統唯一來源 docs/api/error-list.md）。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String OK = "00000";

    public static final String CAMPAIGN_NOT_FOUND = "A0301";
    public static final String STATUS_CONFLICT = "A0302";
    public static final String PROBABILITY_SUM_INVALID = "A0303";
    public static final String PROBABILITY_OUT_OF_RANGE = "A0304";
    public static final String MISSING_THANK_YOU = "A0305";
    public static final String DRAW_LIMIT_EXCEEDED = "A0306";
    public static final String IDEMPOTENCY_CONFLICT = "A0307";
    public static final String STRUCTURAL_INVALID = "A0000";
    public static final String SYSTEM_ERROR = "B0000";

    public static ApiException campaignNotFound() {
        return new ApiException(CAMPAIGN_NOT_FOUND, HttpStatus.NOT_FOUND, "活動不存在");
    }

    public static ApiException statusConflict(String message) {
        return new ApiException(STATUS_CONFLICT, HttpStatus.CONFLICT, message);
    }

    public static ApiException probabilitySumInvalid() {
        return new ApiException(PROBABILITY_SUM_INVALID, HttpStatus.UNPROCESSABLE_ENTITY, "獎品機率總和必須等於 100%");
    }

    public static ApiException probabilityOutOfRange() {
        return new ApiException(PROBABILITY_OUT_OF_RANGE, HttpStatus.UNPROCESSABLE_ENTITY, "獎品機率必須介於 0 到 100");
    }

    public static ApiException missingThankYou() {
        return new ApiException(MISSING_THANK_YOU, HttpStatus.UNPROCESSABLE_ENTITY, "至少需包含一個銘謝惠顧獎品");
    }

    public static ApiException drawLimitExceeded() {
        return new ApiException(DRAW_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS, "個人抽獎次數已達上限");
    }

    public static ApiException idempotencyConflict() {
        return new ApiException(IDEMPOTENCY_CONFLICT, HttpStatus.CONFLICT, "冪等鍵衝突（併發重入）");
    }
}
