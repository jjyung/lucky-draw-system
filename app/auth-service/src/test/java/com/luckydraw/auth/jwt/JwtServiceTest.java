package com.luckydraw.auth.jwt;

import com.luckydraw.contracts.auth.api.model.JwksResourceDTO;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtService 行為測試（單元測試規約：保護不變量，無 DB、無 mock）。
 * 不變量：簽發/驗證隔離（FR-AUTH-03）、claims 正確（AC-AUTH-004）、
 * JWKS 僅含公鑰（AC-AUTH-007/008）。
 */
class JwtServiceTest {

    private JwtService jwtService;
    private JwtKeyProvider keyProvider;

    @BeforeEach
    void setUp() {
        keyProvider = new JwtKeyProvider();
        keyProvider.init();
        jwtService = new JwtService(keyProvider);
    }

    @Test
    @DisplayName("簽發的 JWT 能用 JWKS 公鑰驗證通過（AC-AUTH-007）")
    void issuedToken_isVerifiableByJwksPublicKey() throws Exception {
        String token = jwtService.issueAccessToken(42L, List.of("ROLE_USER"));
        SignedJWT jwt = SignedJWT.parse(token);

        JwksResourceDTO jwksDto = jwtService.jwks();
        RSAKey publicKey = rsaKeyFromJwks(jwksDto);

        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        assertThat(jwt.verify(verifier)).isTrue();
    }

    @Test
    @DisplayName("claims 承載 sub/roles/exp/iat/iss（AC-AUTH-004）")
    void issuedToken_hasExpectedClaims() throws Exception {
        String token = jwtService.issueAccessToken(42L, List.of("ROLE_USER", "ROLE_ADMIN"));
        SignedJWT jwt = SignedJWT.parse(token);

        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("42");
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles"))
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo(keyProvider.getIssuer());
        assertThat(jwt.getJWTClaimsSet().getIssueTime()).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isAfter(jwt.getJWTClaimsSet().getIssueTime());
    }

    @Test
    @DisplayName("JWT header 帶 kid，對應 JWKS 的 kid（多 key 輪替選鑰依據）")
    void tokenKid_matchesJwksKid() throws Exception {
        String token = jwtService.issueAccessToken(42L, List.of("ROLE_USER"));
        SignedJWT jwt = SignedJWT.parse(token);

        JwksResourceDTO jwksDto = jwtService.jwks();
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(jwksDto.getKeys().get(0).getKid());
    }

    @Test
    @DisplayName("JWKS 的 n/e 非空且 kty=RSA（AC-AUTH-008）")
    void jwks_hasRsaKeyMaterial() {
        JwksResourceDTO jwks = jwtService.jwks();

        assertThat(jwks.getKeys()).hasSize(1);
        assertThat(jwks.getKeys().get(0).getKty()).isEqualTo("RSA");
        assertThat(jwks.getKeys().get(0).getN()).isNotBlank();
        assertThat(jwks.getKeys().get(0).getE()).isNotBlank();
    }

    /**
     * 從 JWKS DTO 的 n/e 直接組出 Nimbus RSAKey（避免手組 JSON 的脆弱性）。
     */
    private RSAKey rsaKeyFromJwks(JwksResourceDTO dto) {
        var key = dto.getKeys().get(0);
        Base64URL n = new Base64URL(key.getN());
        Base64URL e = new Base64URL(key.getE());
        return new RSAKey.Builder(n, e).keyID(key.getKid()).build();
    }
}
