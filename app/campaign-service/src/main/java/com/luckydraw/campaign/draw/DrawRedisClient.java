package com.luckydraw.campaign.draw;

/**
 * 抽獎路徑的 Redis 操作抽象（ADR-003/005/006）。
 * 抽象出來讓 DrawService 可單元測試（用 in-memory fake 替代，不真連 Redis）。
 */
public interface DrawRedisClient {

    /**
     * 冪等鎖：SETNX lock:draw:{userId}:{campaignId}:{idemKey} NX PX 30000。
     *
     * @return true=取得鎖；false=鎖被他人持有（併發重入）
     */
    boolean tryLock(Long userId, Long campaignId, String idempotencyKey);

    /**
     * 釋放冪等鎖（僅持有者可釋放）。
     */
    void unlock(Long userId, Long campaignId, String idempotencyKey);

    /**
     * 庫存預扣：Lua 原子 GET + 條件 DECR。
     *
     * @return true=扣減成功；false=庫存不足
     */
    boolean preDeduct(Long prizeId);

    /**
     * 個人抽獎次數 +N（僅成功計次），TTL 對齊活動結束。
     */
    void incrementDrawCount(Long userId, Long campaignId, long by, long ttlSeconds);

    /**
     * 讀取個人已用次數。
     */
    long getDrawCount(Long userId, Long campaignId);
}
