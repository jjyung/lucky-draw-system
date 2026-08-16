package com.luckydraw.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 啟動時把 public JWK 寫入 Redis 共用狀態（ADR-009 修訂：key {@code jwt:public-key}）。
 * 下游服務（gateway / campaign-service）自 Redis 讀取公鑰複驗，不 HTTP 回 auth-service。
 * JWKS endpoint（auth-keys-001）仍公開；此處為內部複驗的公鑰分發通道。
 * 寫入失敗僅記 WARN（graceful degradation，JWKS endpoint 仍是 fallback）。
 */
@Component
public class JwtKeyPublisher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyPublisher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtKeyProvider keyProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${lucky-draw.jwt.redis-key:jwt:public-key}")
    private String redisKey;

    public JwtKeyPublisher(JwtKeyProvider keyProvider, StringRedisTemplate redisTemplate) {
        this.keyProvider = keyProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            RSAKey publicKey = keyProvider.getRsaKey().toPublicJWK();
            Map<String, String> jwk = new LinkedHashMap<>();
            jwk.put("kty", publicKey.getKeyType().getValue());
            jwk.put("kid", publicKey.getKeyID());
            jwk.put("use", "sig");
            jwk.put("alg", "RS256");
            jwk.put("n", publicKey.getModulus().toString());
            jwk.put("e", publicKey.getPublicExponent().toString());
            redisTemplate.opsForValue().set(redisKey, MAPPER.writeValueAsString(jwk));
            log.info("Published JWT public key to Redis key={}", redisKey);
        } catch (Exception e) {
            log.warn("Failed to publish JWT public key to Redis key={} (JWKS endpoint remains as fallback)", redisKey, e);
        }
    }
}
