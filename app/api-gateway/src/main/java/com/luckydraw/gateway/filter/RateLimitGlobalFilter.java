package com.luckydraw.gateway.filter;

import com.luckydraw.gateway.error.GatewayErrorWriter;
import com.luckydraw.gateway.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 限流（UC-2，ADR-003）：per-IP（所有請求）＋ per-user（已驗證）。任一超限 → 429（A0500），不轉發。
 * 回應帶 X-RateLimit-Limit / X-RateLimit-Remaining / X-RateLimit-Reset headers。
 */
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final RateLimiter rateLimiter;
    private final GatewayErrorWriter errorWriter;

    public RateLimitGlobalFilter(RateLimiter rateLimiter, GatewayErrorWriter errorWriter) {
        this.rateLimiter = rateLimiter;
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = resolveClientIp(exchange.getRequest());

        RateLimiter.Result ipResult = rateLimiter.acquire("rate:ip:" + ip, rateLimiter.ipLimit());
        if (!ipResult.allowed()) {
            addRateLimitHeaders(exchange, ipResult);
            return errorWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "A0500", "請求過於頻繁");
        }

        Object userId = exchange.getAttributes().get(JwtAuthenticationGlobalFilter.ATTR_USER_ID);
        if (userId instanceof Long id) {
            RateLimiter.Result userResult = rateLimiter.acquire("rate:user:" + id, rateLimiter.userLimit());
            if (!userResult.allowed()) {
                addRateLimitHeaders(exchange, userResult);
                return errorWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "A0500", "請求過於頻繁");
            }
            addRateLimitHeaders(exchange, userResult);
        } else {
            addRateLimitHeaders(exchange, ipResult);
        }

        return chain.filter(exchange);
    }

    private void addRateLimitHeaders(ServerWebExchange exchange, RateLimiter.Result result) {
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(result.limit()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 1));
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
