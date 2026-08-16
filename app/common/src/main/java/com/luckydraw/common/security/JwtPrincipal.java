package com.luckydraw.common.security;

import java.util.List;

/**
 * JWT 複驗後解出的身份（ADR-009）。
 * 權威身份來源：以各服務自行複驗的憑證 claims 為準，非 Gateway 傳遞的 header（提示）。
 */
public record JwtPrincipal(Long userId, List<String> roles) {
}
