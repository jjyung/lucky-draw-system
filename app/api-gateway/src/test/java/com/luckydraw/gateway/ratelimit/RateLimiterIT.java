package com.luckydraw.gateway.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Gateway 限流固定窗口計數整合測試（ST-GW-003；ADR-003，risk-control.md §4）。
 * 以真 Redis（Testcontainers）驗證 INCR 計數在併發下準確：
 * 窗口 limit=10、20 併發 → 恰 10 放行、10 拒絕（計數不遺失、不超放）。
 */
@Tag("integration")
@Testcontainers
class RateLimiterIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.afterPropertiesSet();
        rateLimiter = new RateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("固定窗口：limit=10、20 併發 → 恰 10 放行、剩餘歸零")
    void concurrentAcquire_exactlyLimitAllowed() throws Exception {
        int limit = 10;
        int m = 20;
        String key = "rate:test:" + System.currentTimeMillis(); // 每測試獨立 key

        ExecutorService pool = Executors.newFixedThreadPool(m);
        CountDownLatch ready = new CountDownLatch(m);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return rateLimiter.acquire(key, limit).allowed();
            }));
        }
        ready.await();
        start.countDown();
        int allowed = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(30, TimeUnit.SECONDS)) {
                allowed++;
            }
        }
        pool.shutdown();

        assertThat(allowed).isEqualTo(limit);       // 恰 limit 放行
    }
}
