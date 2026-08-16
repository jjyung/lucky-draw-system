package com.luckydraw.common.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtVerifier 行為測試（保護安全不變量，純邏輯無 infra）：
 * 驗簽章 + exp + iss + jti 白名單 → sub/roles；竄改/過期/簽發者不符/已撤銷皆拒絕。
 */
class NimbusJwtVerifierTest {

    private RSAKey rsaKey;
    private NimbusJwtVerifier verifier;
    private FakeTokenRegistry tokenRegistry;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        rsaKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("test-key")
                .build();

        JwtKeySource keySource = new JwtKeySource() {
            @Override
            public List<RSAKey> publicKeys() {
                return List.of(rsaKey.toPublicJWK());
            }
        };
        tokenRegistry = new FakeTokenRegistry();
        verifier = new NimbusJwtVerifier(keySource, tokenRegistry, "lucky-draw-system");
    }

    @Test
    @DisplayName("有效 token（簽章通過 + jti 在白名單）→ VALID + sub/roles 解出")
    void validToken_returnsPrincipal() throws Exception {
        String token = issue(42L, List.of("ROLE_USER"), new Date(System.currentTimeMillis() + 60000), "lucky-draw-system");

        JwtVerificationResult result = verifier.verify(token);

        assertThat(result.isValid()).isTrue();
        assertThat(result.principal().userId()).isEqualTo(42L);
        assertThat(result.principal().roles()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("過期 token → EXPIRED（A0202 語意）")
    void expiredToken_returnsExpired() throws Exception {
        String token = issue(42L, List.of("ROLE_USER"), new Date(System.currentTimeMillis() - 1000), "lucky-draw-system");

        JwtVerificationResult result = verifier.verify(token);

        assertThat(result.status()).isEqualTo(JwtVerificationResult.Status.EXPIRED);
    }

    @Test
    @DisplayName("他 key 簽發（竄改）→ INVALID（A0203 語意）")
    void tamperedToken_returnsInvalid() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair other = gen.generateKeyPair();
        RSAKey otherKey = new RSAKey.Builder((RSAPublicKey) other.getPublic())
                .privateKey((RSAPrivateKey) other.getPrivate())
                .keyID("test-key")
                .build();
        String token = signWith(otherKey, 42L, List.of("ROLE_USER"),
                new Date(System.currentTimeMillis() + 60000), "lucky-draw-system");

        JwtVerificationResult result = verifier.verify(token);

        assertThat(result.status()).isEqualTo(JwtVerificationResult.Status.INVALID);
    }

    @Test
    @DisplayName("簽發者不符 → INVALID")
    void wrongIssuer_returnsInvalid() throws Exception {
        String token = issue(42L, List.of("ROLE_USER"), new Date(System.currentTimeMillis() + 60000), "other-issuer");

        JwtVerificationResult result = verifier.verify(token);

        assertThat(result.status()).isEqualTo(JwtVerificationResult.Status.INVALID);
    }

    @Test
    @DisplayName("已登出（jti 不在白名單）→ REVOKED，即使簽章有效")
    void revokedToken_returnsRevoked() throws Exception {
        String token = issue(42L, List.of("ROLE_USER"), new Date(System.currentTimeMillis() + 60000), "lucky-draw-system");
        tokenRegistry.revoke("42", SignedJWT.parse(token).getJWTClaimsSet().getJWTID());

        JwtVerificationResult result = verifier.verify(token);

        assertThat(result.status()).isEqualTo(JwtVerificationResult.Status.REVOKED);
    }

    @Test
    @DisplayName("缺 token / 空白 → INVALID")
    void blankToken_returnsInvalid() {
        assertThat(verifier.verify(null).status()).isEqualTo(JwtVerificationResult.Status.INVALID);
        assertThat(verifier.verify("  ").status()).isEqualTo(JwtVerificationResult.Status.INVALID);
    }

    private String issue(Long userId, List<String> roles, Date exp, String issuer) throws Exception {
        return signWith(rsaKey, userId, roles, exp, issuer);
    }

    private String signWith(RSAKey key, Long userId, List<String> roles, Date exp, String issuer) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("roles", roles)
                .issuer(issuer)
                .issueTime(new Date())
                .expirationTime(exp)
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    /** in-memory fake（classical）：revoked 集合內的 jti 視為已登出。 */
    static class FakeTokenRegistry implements TokenRegistry {
        private final Set<String> revoked = new HashSet<>();

        @Override
        public void register(String userId, String jti, long ttlSeconds) {
            revoked.remove(jti);
        }

        @Override
        public void revoke(String userId, String jti) {
            revoked.add(jti);
        }

        @Override
        public boolean isActive(String jti) {
            return !revoked.contains(jti);
        }
    }
}
