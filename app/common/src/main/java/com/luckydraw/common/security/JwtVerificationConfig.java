package com.luckydraw.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * JWT 複驗共用元件裝配（ADR-009 §3）。
 * 各服務以 {@code @Import(JwtVerificationConfig.class)} 引入；
 * 需要 spring-boot-starter-data-redis（StringRedisTemplate 由該 starter auto-config）。
 */
@Configuration
public class JwtVerificationConfig {

    @Bean
    @ConditionalOnMissingBean(JwtKeySource.class)
    public JwtKeySource jwtKeySource(
            StringRedisTemplate redisTemplate,
            @Value("${lucky-draw.jwt.redis-key:jwt:public-key}") String redisKey,
            @Value("${lucky-draw.jwt.key-ttl-seconds:60}") long ttlSeconds) {
        return new RedisJwtKeySource(redisTemplate, redisKey, ttlSeconds * 1000L);
    }

    @Bean
    @ConditionalOnMissingBean(TokenRegistry.class)
    public TokenRegistry tokenRegistry(
            StringRedisTemplate redisTemplate,
            @Value("${lucky-draw.auth.max-sessions:5}") int maxSessions) {
        return new RedisTokenRegistry(redisTemplate, maxSessions);
    }

    @Bean
    @ConditionalOnMissingBean(JwtVerifier.class)
    public JwtVerifier jwtVerifier(
            JwtKeySource keySource,
            TokenRegistry tokenRegistry,
            @Value("${lucky-draw.jwt.issuer:lucky-draw-system}") String issuer) {
        return new NimbusJwtVerifier(keySource, tokenRegistry, issuer);
    }
}
