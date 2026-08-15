package com.luckydraw.auth.jwt;

import com.luckydraw.contracts.auth.api.model.JwkResourceDTO;
import com.luckydraw.contracts.auth.api.model.JwksResourceDTO;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT RS256 簽發與 JWKS 公開（ADR-009；auth-keys-001）。
 * 簽發/驗證隔離（FR-AUTH-03）：此處僅簽發（持有 private key），
 * 驗證方以 JWKS 公鑰獨立複驗。
 */
@Service
public class JwtService {

    private final JwtKeyProvider keyProvider;

    public JwtService(JwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * 簽發存取憑證。claims 承載 sub（userId）、roles、exp、iat、iss、jti（ADR-009）。
     */
    public String issueAccessToken(Long userId, List<String> roles) {
        try {
            RSAKey rsaKey = keyProvider.getRsaKey();
            long now = System.currentTimeMillis();
            long ttl = keyProvider.getTtlSeconds() * 1000L;

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("roles", roles)
                    .issuer(keyProvider.getIssuer())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + ttl))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue access token", e);
        }
    }

    /**
     * JWKS 公開（僅 public key；private key 不可經任何端點取得，AC-AUTH-007）。
     */
    public JwksResourceDTO jwks() {
        RSAKey rsaKey = keyProvider.getRsaKey();
        RSAKey publicKey = rsaKey.toPublicJWK();

        JwkResourceDTO jwk = new JwkResourceDTO()
                .kty(publicKey.getKeyType().getValue())
                .kid(publicKey.getKeyID())
                .use(publicKey.getKeyUse() != null ? publicKey.getKeyUse().getValue() : "sig")
                .alg(publicKey.getAlgorithm() != null ? publicKey.getAlgorithm().getName() : "RS256")
                .n(publicKey.getModulus().toString())
                .e(publicKey.getPublicExponent().toString());

        JwksResourceDTO jwks = new JwksResourceDTO();
        jwks.addKeysItem(jwk);
        return jwks;
    }
}
