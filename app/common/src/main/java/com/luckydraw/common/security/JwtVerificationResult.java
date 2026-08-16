package com.luckydraw.common.security;

/**
 * JWT 驗證結果（區分有效／過期／無效，供 gateway 對映 A0202/A0203、下游決定 401）。
 */
public record JwtVerificationResult(JwtPrincipal principal, Status status) {

    public enum Status {
        VALID, EXPIRED, INVALID, REVOKED
    }

    public boolean isValid() {
        return status == Status.VALID;
    }

    public static JwtVerificationResult valid(JwtPrincipal principal) {
        return new JwtVerificationResult(principal, Status.VALID);
    }

    public static JwtVerificationResult expired() {
        return new JwtVerificationResult(null, Status.EXPIRED);
    }

    public static JwtVerificationResult invalid() {
        return new JwtVerificationResult(null, Status.INVALID);
    }

    public static JwtVerificationResult revoked() {
        return new JwtVerificationResult(null, Status.REVOKED);
    }
}
