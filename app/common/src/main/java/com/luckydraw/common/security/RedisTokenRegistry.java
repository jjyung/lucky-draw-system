package com.luckydraw.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * TokenRegistry 的 Redis 實作（ADR-009 修訂）。
 * - 扁平 key {@code auth:token:{jti}} = userId（TTL 對齊 token 有效期），驗證 O(1) 查。
 * - per-user 集合 {@code auth:sessions:{userId}}（ZSET，score = 簽發時間），ZCARD 即同時登入數。
 * - 登入以 Lua 原子：清理已過期成員 → ZADD → 超限踢最舊（FIFO，並 DEL 其扁平 key）。
 */
public class RedisTokenRegistry implements TokenRegistry {

    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String SESSION_PREFIX = "auth:sessions:";

    /** KEYS[1]=session key; ARGV[1]=token prefix; ARGV[2]=score; ARGV[3]=jti; ARGV[4]=maxSessions */
    private static final DefaultRedisScript<Long> REGISTER_SCRIPT = new DefaultRedisScript<>(
            "local members = redis.call('ZRANGE', KEYS[1], 0, -1) " +
            "local stale = {} " +
            "for i, m in ipairs(members) do " +
            "  if redis.call('EXISTS', ARGV[1] .. m) == 0 then table.insert(stale, m) end " +
            "end " +
            "if #stale > 0 then redis.call('ZREM', KEYS[1], unpack(stale)) end " +
            "redis.call('ZADD', KEYS[1], ARGV[2], ARGV[3]) " +
            "local size = redis.call('ZCARD', KEYS[1]) " +
            "if size > tonumber(ARGV[4]) then " +
            "  local overflow = redis.call('ZRANGE', KEYS[1], 0, size - tonumber(ARGV[4]) - 1) " +
            "  for i, m in ipairs(overflow) do redis.call('DEL', ARGV[1] .. m) end " +
            "  if #overflow > 0 then redis.call('ZREM', KEYS[1], unpack(overflow)) end " +
            "end " +
            "return 1", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int maxSessions;
    private final Clock clock;

    public RedisTokenRegistry(StringRedisTemplate redisTemplate, int maxSessions) {
        this(redisTemplate, maxSessions, Clock.systemUTC());
    }

    RedisTokenRegistry(StringRedisTemplate redisTemplate, int maxSessions, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.maxSessions = maxSessions;
        this.clock = clock;
    }

    @Override
    public void register(String userId, String jti, long ttlSeconds) {
        String tokenKey = TOKEN_PREFIX + jti;
        String sessionKey = SESSION_PREFIX + userId;
        // 1. 扁平 key（驗證 O(1) 查）
        redisTemplate.opsForValue().set(tokenKey, userId, Duration.ofSeconds(ttlSeconds));
        // 2. session 集合（清理過期 + 加入 + 超限踢最舊，原子）
        redisTemplate.execute(REGISTER_SCRIPT, List.of(sessionKey),
                TOKEN_PREFIX, String.valueOf(clock.millis()), jti, String.valueOf(maxSessions));
    }

    @Override
    public void revoke(String userId, String jti) {
        redisTemplate.delete(TOKEN_PREFIX + jti);
        redisTemplate.opsForZSet().remove(SESSION_PREFIX + userId, jti);
    }

    @Override
    public boolean isActive(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + jti));
    }
}
