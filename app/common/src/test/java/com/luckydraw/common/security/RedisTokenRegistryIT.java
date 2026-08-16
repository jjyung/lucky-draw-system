package com.luckydraw.common.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * token 白名單「踢最舊（FIFO）」整合測試（ADR-009 修訂）。
 * 以真 Redis（Testcontainers）驗證 RedisTokenRegistry 的 Lua 腳本：超限踢最舊、登出撤銷。
 */
@Tag("integration")
@Testcontainers
class RedisTokenRegistryIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private StringRedisTemplate redisTemplate;

    @BeforeAll
    static void startContainer() {
        REDIS.start();
    }

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.afterPropertiesSet();
    }

    @Test
    @DisplayName("超限（max=3）→ 第 4 個登入踢掉最舊的 jti（FIFO）")
    void register_overLimit_kicksOldest() {
        RedisTokenRegistry registry = new RedisTokenRegistry(redisTemplate, 3, new IncrementingClock());

        registry.register("user-A", "jti-1", 600);
        registry.register("user-A", "jti-2", 600);
        registry.register("user-A", "jti-3", 600);
        registry.register("user-A", "jti-4", 600); // 第 4 個 → 踢最舊 jti-1

        assertThat(registry.isActive("jti-1")).isFalse(); // 被踢
        assertThat(registry.isActive("jti-2")).isTrue();
        assertThat(registry.isActive("jti-3")).isTrue();
        assertThat(registry.isActive("jti-4")).isTrue();
    }

    @Test
    @DisplayName("未超限 → 全數保留")
    void register_withinLimit_keepsAll() {
        RedisTokenRegistry registry = new RedisTokenRegistry(redisTemplate, 5, new IncrementingClock());

        registry.register("user-B", "jti-1", 600);
        registry.register("user-B", "jti-2", 600);

        assertThat(registry.isActive("jti-1")).isTrue();
        assertThat(registry.isActive("jti-2")).isTrue();
    }

    @Test
    @DisplayName("登出 → revoke 後 token 失效")
    void revoke_removesFromWhitelist() {
        RedisTokenRegistry registry = new RedisTokenRegistry(redisTemplate, 5, new IncrementingClock());

        registry.register("user-C", "jti-1", 600);
        assertThat(registry.isActive("jti-1")).isTrue();

        registry.revoke("user-C", "jti-1");
        assertThat(registry.isActive("jti-1")).isFalse();
    }

    @AfterAll
    static void stopContainer() {
        REDIS.stop();
    }

    /** 遞增時間源：讓 ZSET score 嚴格遞增，踢最舊可確定。 */
    static class IncrementingClock extends Clock {
        private final AtomicLong millis = new AtomicLong(1_000_000L);

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }

        @Override
        public long millis() {
            return millis.getAndIncrement();
        }
    }
}
