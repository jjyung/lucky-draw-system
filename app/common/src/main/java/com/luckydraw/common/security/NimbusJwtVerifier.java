package com.luckydraw.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;
import java.util.List;

/**
 * Nimbus RS256 JWT 複驗（ADR-009 §3，含 token 白名單修訂）。
 * 驗證順序：parse → 依 kid 選鑰驗簽章 → exp 時效 → iss 簽發者 → jti 白名單（in Redis）→ 解 sub/roles。
 * 簽章失敗會呼叫 {@link JwtKeySource#refresh()} 一次並重試（容納 key 輪替）。
 * 白名單檢查（fail-closed）：簽章有效但 jti 不在白名單（已登出/撤銷）→ REVOKED。
 */
public class NimbusJwtVerifier implements JwtVerifier {

    private final JwtKeySource keySource;
    private final TokenRegistry tokenRegistry;
    private final String issuer;

    public NimbusJwtVerifier(JwtKeySource keySource, TokenRegistry tokenRegistry, String issuer) {
        this.keySource = keySource;
        this.tokenRegistry = tokenRegistry;
        this.issuer = issuer;
    }

    @Override
    public JwtVerificationResult verify(String token) {
        if (token == null || token.isBlank()) {
            return JwtVerificationResult.invalid();
        }
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (Exception e) {
            return JwtVerificationResult.invalid();
        }

        boolean signatureOk = verifySignature(jwt, keySource.publicKeys());
        if (!signatureOk) {
            // key 可能已輪替：刷新一次再重試
            keySource.refresh();
            signatureOk = verifySignature(jwt, keySource.publicKeys());
        }
        if (!signatureOk) {
            return JwtVerificationResult.invalid();
        }

        try {
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                return JwtVerificationResult.expired();
            }
            if (issuer != null && !issuer.isEmpty() && !issuer.equals(claims.getIssuer())) {
                return JwtVerificationResult.invalid();
            }
            // token 白名單（in Redis && 驗章才通過，fail-closed；支援登出/撤銷）
            String jti = claims.getJWTID();
            if (jti == null || jti.isBlank()) {
                return JwtVerificationResult.invalid();
            }
            if (!tokenRegistry.isActive(jti)) {
                return JwtVerificationResult.revoked();
            }
            Long userId = Long.valueOf(claims.getSubject());
            List<String> roles = claims.getStringListClaim("roles");
            return JwtVerificationResult.valid(new JwtPrincipal(userId, roles == null ? List.of() : roles));
        } catch (Exception e) {
            return JwtVerificationResult.invalid();
        }
    }

    private boolean verifySignature(SignedJWT jwt, List<RSAKey> keys) {
        String kid = jwt.getHeader().getKeyID();
        for (RSAKey key : keys) {
            if (kid != null && !kid.isEmpty() && !kid.equals(key.getKeyID())) {
                continue;
            }
            try {
                if (jwt.verify(new RSASSAVerifier(key))) {
                    return true;
                }
            } catch (JOSEException ignored) {
                // 嘗試下一把 key
            }
        }
        return false;
    }
}
