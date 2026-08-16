package com.luckydraw.inventory.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * InventoryRedisClient 的 Redis 實作（stock:{prizeId} 即時判定層，ADR-006）。
 */
@Component
public class RedisInventoryClient implements InventoryRedisClient {

    private final StringRedisTemplate redisTemplate;

    public RedisInventoryClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void incrementStock(Long prizeId, int quantity) {
        redisTemplate.opsForValue().increment("stock:" + prizeId, quantity);
    }

    @Override
    public void setStock(Long prizeId, int stock) {
        redisTemplate.opsForValue().set("stock:" + prizeId, String.valueOf(stock));
    }
}
