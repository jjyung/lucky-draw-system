package com.luckydraw.common.security;

/**
 * JWT 驗證抽象（ADR-009：各服務獨立複驗，defense in depth）。
 * 實作以 public key 驗 RS256 簽章 + exp 時效 + iss 簽發者，回傳身份（sub/roles）。
 */
public interface JwtVerifier {

    /**
     * 驗證 bearer token。
     *
     * @return 驗證結果（VALID 附身份；EXPIRED 過期；INVALID 簽章/格式/iss 不符）。
     */
    JwtVerificationResult verify(String token);
}
