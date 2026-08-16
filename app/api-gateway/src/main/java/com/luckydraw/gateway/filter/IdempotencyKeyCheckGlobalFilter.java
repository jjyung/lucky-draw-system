package com.luckydraw.gateway.filter;

import com.luckydraw.gateway.error.GatewayErrorWriter;
import com.luckydraw.gateway.route.GatewayRoutes;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 冪等 header 存在性檢查（UC-3，ADR-005）：僅抽獎路徑要求 Idempotency-Key。
 * 只做存在性檢查，不驗證格式、不去重（冪等真正語意在 campaign-service）。缺 → 400（A0501）。
 */
@Component
public class IdempotencyKeyCheckGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayErrorWriter errorWriter;

    public IdempotencyKeyCheckGlobalFilter(GatewayErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (GatewayRoutes.isDraw(exchange.getRequest())) {
            String key = exchange.getRequest().getHeaders().getFirst("Idempotency-Key");
            if (key == null || key.isBlank()) {
                return errorWriter.write(exchange, HttpStatus.BAD_REQUEST, "A0501", "缺少 Idempotency-Key");
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -80;
    }
}
