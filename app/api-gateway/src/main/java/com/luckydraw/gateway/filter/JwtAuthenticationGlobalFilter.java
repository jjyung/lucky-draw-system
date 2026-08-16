package com.luckydraw.gateway.filter;

import com.luckydraw.common.security.JwtPrincipal;
import com.luckydraw.common.security.JwtVerificationResult;
import com.luckydraw.common.security.JwtVerifier;
import com.luckydraw.gateway.error.GatewayErrorWriter;
import com.luckydraw.gateway.route.GatewayRoutes;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT 複驗（UC-1，ADR-009）：受保護路徑需有效 JWT（RS256）。
 * 驗證成功 → 注入 X-User-Id / X-User-Roles（提示）＋ 保留 Authorization（供下游獨立複驗）。
 * 無效/過期/缺憑證 → 401（A0202/A0203），不轉發。公開路徑跳過。
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String ATTR_USER_ID = "gateway.userId";

    private final JwtVerifier jwtVerifier;
    private final GatewayErrorWriter errorWriter;

    public JwtAuthenticationGlobalFilter(JwtVerifier jwtVerifier, GatewayErrorWriter errorWriter) {
        this.jwtVerifier = jwtVerifier;
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (GatewayRoutes.isPublic(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "A0203", "憑證無效");
        }

        JwtVerificationResult result = jwtVerifier.verify(header.substring(7));
        if (result.isValid()) {
            JwtPrincipal principal = result.principal();
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(principal.userId()))
                    .header("X-User-Roles", String.join(",", principal.roles()))
                    .build();
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();
            mutatedExchange.getAttributes().put(ATTR_USER_ID, principal.userId());
            return chain.filter(mutatedExchange);
        }

        if (result.status() == JwtVerificationResult.Status.EXPIRED) {
            return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "A0202", "憑證過期");
        }
        if (result.status() == JwtVerificationResult.Status.REVOKED) {
            return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "A0203", "憑證已失效");
        }
        return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "A0203", "憑證無效");
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
