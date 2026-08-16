package com.luckydraw.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckydraw.common.security.JwtPrincipal;
import com.luckydraw.common.security.JwtVerificationResult;
import com.luckydraw.common.security.JwtVerifier;
import com.luckydraw.gateway.error.GatewayErrorWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * JWT 驗證 GlobalFilter 測試（ST-GW-001/002，AC-GW-001~004）。
 * 不變量：公開路徑放行；受保護路徑需有效 JWT，成功注入 X-User-Id/X-User-Roles（保留 Authorization）；
 * 無效/過期/已撤銷 → 401（A0203/A0202），不轉發。
 */
class JwtAuthenticationGlobalFilterTest {

    private FakeJwtVerifier jwtVerifier;
    private JwtAuthenticationGlobalFilter filter;

    @BeforeEach
    void setUp() {
        jwtVerifier = new FakeJwtVerifier();
        filter = new JwtAuthenticationGlobalFilter(jwtVerifier,
                new GatewayErrorWriter(new ObjectMapper()));
    }

    @Test
    @DisplayName("公開路徑 → 放行、不驗證（AC-GW-010）")
    void publicPath_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/campaigns"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.filtered).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("有效 token → 注入 X-User-Id / X-User-Roles、保留 Authorization（AC-GW-004）")
    void validToken_injectsIdentityHeaders() {
        jwtVerifier.result = JwtVerificationResult.valid(new JwtPrincipal(42L, List.of("ROLE_USER")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/campaigns/5/draw")
                        .header("Authorization", "Bearer valid-token"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.filtered).isTrue();
        assertThat(chain.exchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        assertThat(chain.exchange.getRequest().getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_USER");
        assertThat(chain.exchange.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer valid-token");
        assertThat(chain.exchange.getAttributes().get(JwtAuthenticationGlobalFilter.ATTR_USER_ID)).isEqualTo(42L);
    }

    @Test
    @DisplayName("缺 token → 401/A0203、不轉發（AC-GW-001）")
    void missingToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/campaigns/5/draw"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.filtered).isFalse();
    }

    @Test
    @DisplayName("憑證無效 → 401/A0203（AC-GW-002）")
    void invalidToken_returns401() {
        jwtVerifier.result = JwtVerificationResult.invalid();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/campaigns/5/draw")
                        .header("Authorization", "Bearer bad-token"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.filtered).isFalse();
    }

    @Test
    @DisplayName("憑證過期 → 401（A0202 語意）")
    void expiredToken_returns401() {
        jwtVerifier.result = JwtVerificationResult.expired();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/campaigns/5/draw")
                        .header("Authorization", "Bearer expired-token"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.filtered).isFalse();
    }

    @Test
    @DisplayName("已撤銷 token → 401/A0203，不轉發")
    void revokedToken_returns401() {
        jwtVerifier.result = JwtVerificationResult.revoked();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/campaigns/5/draw")
                        .header("Authorization", "Bearer revoked-token"));
        RecordingChain chain = new RecordingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.filtered).isFalse();
    }

    /** 記錄 filter 是否轉發、以及轉發的 exchange（供斷言身份 header）。 */
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

    static class FakeJwtVerifier implements JwtVerifier {
        JwtVerificationResult result = JwtVerificationResult.invalid();

        @Override
        public JwtVerificationResult verify(String token) {
            return result;
        }
    }
}
