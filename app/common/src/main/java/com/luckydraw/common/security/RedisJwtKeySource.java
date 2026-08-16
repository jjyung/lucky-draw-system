package com.luckydraw.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 自 Redis 共用狀態讀取 JWT 驗證公鑰（ADR-009 修訂：key {@code jwt:public-key}）。
 * auth-service 啟動時寫入 public JWK（JSON：kty/kid/n/e）；本類讀取 + in-memory 短 TTL 快取，
 * 避免每個請求都打 Redis 或 HTTP 回 auth-service。
 */
public class RedisJwtKeySource implements JwtKeySource {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;
    private final String redisKey;
    private final long cacheTtlMillis;

    private volatile List<RSAKey> cached = List.of();
    private volatile long cachedAt = 0L;

    public RedisJwtKeySource(StringRedisTemplate redisTemplate, String redisKey, long cacheTtlMillis) {
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
        this.cacheTtlMillis = cacheTtlMillis;
    }

    @Override
    public List<RSAKey> publicKeys() {
        if (cached.isEmpty() || System.currentTimeMillis() - cachedAt > cacheTtlMillis) {
            refresh();
        }
        return cached;
    }

    @Override
    public void refresh() {
        this.cached = load();
        this.cachedAt = System.currentTimeMillis();
    }

    private List<RSAKey> load() {
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json == null || json.isBlank()) {
                return List.of();
            }
            JsonNode node = MAPPER.readTree(json);
            String kid = node.path("kid").asText(null);
            String n = node.path("n").asText(null);
            String e = node.path("e").asText(null);
            if (n == null || e == null) {
                return List.of();
            }
            RSAKey.Builder builder = new RSAKey.Builder(new Base64URL(n), new Base64URL(e));
            if (kid != null && !kid.isBlank()) {
                builder.keyID(kid);
            }
            return List.of(builder.build());
        } catch (Exception e) {
            return List.of();
        }
    }
}
