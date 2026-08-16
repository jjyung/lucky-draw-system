package com.luckydraw.common.security;

/**
 * 簽發 token 的白名單註冊表（ADR-009 修訂：token 白名單，支援登出/撤銷 + 同時登入數量控管）。
 * - 登入簽發 → {@link #register}（per-user 集合，超限踢最舊 FIFO）；登出/撤銷 → {@link #revoke}；
 *   驗證 → {@link #isActive}。
 * - 驗證語意：簽章通過 **且** jti 仍在白名單（in Redis）才通過（fail-closed）。
 */
public interface TokenRegistry {

    /**
     * 登入簽發後註冊：寫扁平 key（TTL 對齊 token 有效期）＋加進 per-user session 集合；
     * 集合規模超過 max-sessions → 踢最舊（FIFO）。
     */
    void register(String userId, String jti, long ttlSeconds);

    /**
     * 登出/撤銷：移除扁平 key + 從 per-user session 集合移除。
     */
    void revoke(String userId, String jti);

    /**
     * token 是否仍在白名單（未登出、未過期、未被踢）。
     */
    boolean isActive(String jti);
}
