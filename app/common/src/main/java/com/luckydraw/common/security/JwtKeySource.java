package com.luckydraw.common.security;

import com.nimbusds.jose.jwk.RSAKey;

import java.util.List;

/**
 * JWT 驗證公鑰來源（ADR-009 §3 修訂：自 Redis 共用狀態讀取，不 HTTP 回 auth-service）。
 * 抽象出來讓 JwtVerifier 可單元測試（用固定 key 替換，不真連 Redis）。
 */
public interface JwtKeySource {

    /**
     * 取得驗證公鑰（依 kid 選鑰；POC 單 key）。
     */
    List<RSAKey> publicKeys();

    /**
     * 強制刷新（key 輪替時由 verifier 在簽章失敗後呼叫一次，再重試）。
     */
    default void refresh() {
    }
}
