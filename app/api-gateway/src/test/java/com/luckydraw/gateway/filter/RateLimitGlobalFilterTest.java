package com.luckydraw.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckydraw.gateway.error.GatewayErrorWriter;
import com.luckydraw.gateway.ratelimit.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 限流 GlobalFilter 測試（ST-GW-003，AC-GW-006/007）。
 * 不變量：per-IP 先、per-user 後，任一超限 → 429 不轉發；未超限 → 放行並帶 X-RateLimit-* headers。
 */
class RateLimitGlobalFilterTest {

    private RateLimiter rateLimiter;
    private RateLimitGlobalFilter filter;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.ipLimit()).thenReturn(100);
        when(rateLimiter.userLimit()).thenReturn(10);
        filter = new RateLimitGlobalFilter(rateLimiter, new GatewayErrorWriter(new ObjectMapper()));
    }

    @Test
    @DisplayName("per-IP 超限 → 429、不轉發（AC-GW-006）")
    void perIpOverLimit_returns429() {
        when(rateLimiter.acquire(startsWith("rate:ip:"), eq(100)))
                .thenReturn(new RateLimiter.Result(false, 0, 100));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/campaigns").header("X-Forwarded-For", "1.2.3.4"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chain.filtered).isFalse();
    }

    @Test
    @DisplayName("per-user 超限（已驗證身份）→ 429（AC-GW-007）")
    void perUserOverLimit_returns429() {
        when(rateLimiter.acquire(startsWith("rate:ip:"), eq(100)))
                .thenReturn(new RateLimiter.Result(true, 99, 100));
        when(rateLimiter.acquire(startsWith("rate:user:"), eq(10)))
                .thenReturn(new RateLimiter.Result(false, 0, 10));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/campaigns").header("X-Forwarded-For", "1.2.3.4"));
        exchange.getAttributes().put(JwtAuthenticationGlobalFilter.ATTR_USER_ID, 42L);
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chain.filtered).isFalse();
    }

    @Test
    @DisplayName("未超限 → 放行並帶 X-RateLimit-* headers")
    void withinLimit_passesThrough_withHeaders() {
        when(rateLimiter.acquire(startsWith("rate:ip:"), eq(100)))
                .thenReturn(new RateLimiter.Result(true, 99, 100));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/campaigns").header("X-Forwarded-For", "1.2.3.4"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.filtered).isTrue();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("99");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    static class RecordingChain implements GatewayFilterChain {
        ServerWebExchange exchange;
        boolean filtered;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.exchange = exchange;
            this.filtered = true;
            return Mono.empty();
        }
    }
}
