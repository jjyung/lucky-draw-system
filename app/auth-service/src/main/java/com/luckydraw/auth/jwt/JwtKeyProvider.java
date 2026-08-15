package com.luckydraw.auth.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * JWT 簽章金鑰提供者（ADR-009）。
 * prod：private key 由 Secret Manager 注入（PEM 字串，經環境變數），kid 亦由部署指定。
 * dev：未配置 PEM 時，於啟動生成一組 RSA 2048 key（memory），kid 隨機；
 *      驗證方本就透過 JWKS 端點取公鑰（auth-keys-001），不 hardcode 公鑰，故隨機 key 不影響協作。
 */
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtKeyProvider {

    private String kid;
    private String privateKeyPem;
    private String issuer = "lucky-draw-system";
    private long ttlSeconds = 1800;

    private RSAKey rsaKey;

    @PostConstruct
    public void init() {
        this.rsaKey = loadOrGenerate();
    }

    private RSAKey loadOrGenerate() {
        try {
            if (privateKeyPem != null && !privateKeyPem.isBlank()) {
                RSAKey parsed = RSAKey.parse(privateKeyPem);
                return new RSAKey.Builder(parsed)
                        .keyID(kid != null ? kid : "key-1")
                        .build();
            }
            // dev：生成隨機 key
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(kid != null ? kid : "dev-" + UUID.randomUUID())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load or generate JWT signing key", e);
        }
    }

    public RSAKey getRsaKey() {
        return rsaKey;
    }

    public String getKid() {
        return rsaKey.getKeyID();
    }

    public String getIssuer() {
        return issuer;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
