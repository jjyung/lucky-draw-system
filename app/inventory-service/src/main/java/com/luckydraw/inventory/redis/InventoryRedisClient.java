package com.luckydraw.inventory.redis;

/**
 * inventory-service 對 Redis 即時判定層（stock:{prizeId}）的操作抽象（ADR-006 / risk-control.md §1）。
 * 抽象出來讓補償/校對邏輯可單元測試（用 in-memory fake 替代，不真連 Redis）。
 */
public interface InventoryRedisClient {

    /**
     * 加回即時判定層額度（補償 UC-2 / 超時回收 UC-3）。
     */
    void incrementStock(Long prizeId, int quantity);

    /**
     * 以 DB 真相校正即時判定層（帳目校對 UC-3）。
     */
    void setStock(Long prizeId, int stock);
}
