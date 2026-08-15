package com.luckydraw.campaign.draw;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * DrawRedisClient 的 Redis 實作（ADR-003/005/006）。
 * 冪等鎖 SETNX、庫存預扣 Lua、計次 INCR + TTL。
 */
@Component
public class RedisDrawClient implements DrawRedisClient {

    private static final long LOCK_TTL_MS = 30000L;

    /** 庫存預扣 Lua：GET + 條件 DECR（原子） */
    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            "local s = tonumber(redis.call('GET', KEYS[1]) or '0') " +
            "if s > 0 then redis.call('DECR', KEYS[1]) return 1 " +
            "else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDrawClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(Long userId, Long campaignId, String idempotencyKey) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                lockKey(userId, campaignId, idempotencyKey), "1", Duration.ofMillis(LOCK_TTL_MS));
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void unlock(Long userId, Long campaignId, String idempotencyKey) {
        redisTemplate.delete(lockKey(userId, campaignId, idempotencyKey));
    }

    @Override
    public boolean preDeduct(Long prizeId) {
        Long result = redisTemplate.execute(PRE_DEDUCT_SCRIPT, List.of("stock:" + prizeId));
        return result != null && result == 1L;
    }

    @Override
    public void incrementDrawCount(Long userId, Long campaignId, long by, long ttlSeconds) {
        String key = drawCountKey(userId, campaignId);
        Long current = redisTemplate.opsForValue().increment(key, by);
        if (current != null && current == by) {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
    }

    @Override
    public long getDrawCount(Long userId, Long campaignId) {
        String v = redisTemplate.opsForValue().get(drawCountKey(userId, campaignId));
        return v == null ? 0L : Long.parseLong(v);
    }

    private String lockKey(Long userId, Long campaignId, String idempotencyKey) {
        return "lock:draw:" + userId + ":" + campaignId + ":" + idempotencyKey;
    }

    private String drawCountKey(Long userId, Long campaignId) {
        return "draw_count:" + userId + ":" + campaignId;
    }
}
