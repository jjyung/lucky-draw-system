package com.luckydraw.campaign.draw;

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
 * 熱點庫存預扣 Lua 原子性整合測試（FR-CAMP-18/19，ST-CAMP-009；ADR-003/006）。
 * 以真 Redis（Testcontainers）驗證「GET + if > 0 DECR」Lua 在併發下原子：
 * 庫存 5、10 併發預扣 → 恰 5 成功、庫存歸 0 不為負（加速層絕不超扣）。
 */
@Tag("integration")
@Testcontainers
class RedisPreDeductIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private StringRedisTemplate redisTemplate;
    private RedisDrawClient redisDrawClient;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.afterPropertiesSet();
        redisDrawClient = new RedisDrawClient(redisTemplate);
    }

    @Test
    @DisplayName("預扣 Lua：庫存 5、10 併發預扣 → 恰 5 成功、庫存歸 0 不為負")
    void concurrentPreDeduct_neverNegative() throws Exception {
        Long prizeId = 99L;
        String key = "stock:" + prizeId;
        redisTemplate.opsForValue().set(key, "5"); // 種子庫存 5

        int m = 10;
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
                return redisDrawClient.preDeduct(prizeId);
            }));
        }
        ready.await();
        start.countDown();
        int success = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(30, TimeUnit.SECONDS)) {
                success++;
            }
        }
        pool.shutdown();

        assertThat(success).isEqualTo(5);                           // 恰 5 成功
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("0"); // 不為負
    }
}
