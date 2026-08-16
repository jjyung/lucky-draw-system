package com.luckydraw.gateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 固定窗口限流（ADR-003，risk-control.md §4）：INCR + 首次 EXPIRE。
 * 固定窗口在窗口邊界有 2 倍 burst 理論缺口；對 POC 足夠（risk-control.md §4.2）。
 */
@Component
public class RateLimiter {

    public record Result(boolean allowed, long remaining, long limit) {
    }

    private final StringRedisTemplate redisTemplate;

    @Value("${gateway.rate-limit.user-rps:10}")
    private int userRps;

    @Value("${gateway.rate-limit.ip-rps:100}")
    private int ipRps;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int userLimit() {
        return userRps;
    }

    public int ipLimit() {
        return ipRps;
    }

    /**
     * 對指定 key 計數（1 秒窗口）。回傳是否放行與剩餘額度。
     */
    public Result acquire(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(1));
        }
        long c = count == null ? 0L : count;
        return new Result(c <= limit, Math.max(0, limit - c), limit);
    }
}
