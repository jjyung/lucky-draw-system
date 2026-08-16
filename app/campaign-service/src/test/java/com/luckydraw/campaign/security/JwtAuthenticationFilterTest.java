package com.luckydraw.campaign.security;

import com.luckydraw.common.security.JwtPrincipal;
import com.luckydraw.common.security.JwtVerificationResult;
import com.luckydraw.common.security.JwtVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * campaign JWT 複驗 filter 測試（ST-AUTH-004 權限分級的基礎，defense in depth）。
 * 不變量：有效 token → SecurityContext 承載 sub(userId) + roles（供 SecurityConfig hasRole 授權）；
 * 無效/過期 → 不設認證（401）；不信任 X-User-Id header，以複驗 claims 為準。
 */
class JwtAuthenticationFilterTest {

    private FakeJwtVerifier jwtVerifier;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtVerifier = new FakeJwtVerifier();
        filter = new JwtAuthenticationFilter(jwtVerifier);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("有效 token → SecurityContext 承載 userId + roles（供 hasRole 授權）")
    void validToken_setsSecurityContext() throws Exception {
        jwtVerifier.result = JwtVerificationResult.valid(new JwtPrincipal(42L, List.of("ROLE_USER", "ROLE_ADMIN")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(42L);
        assertThat(auth.getAuthorities())
                .extracting(a -> a.getAuthority())
                .contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("無效 token → 不設認證（401）")
    void invalidToken_noAuthentication() throws Exception {
        jwtVerifier.result = JwtVerificationResult.invalid();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("過期 token → 不設認證、標記 ATTR_EXPIRED（entry point 回 A0202）")
    void expiredToken_marksAttribute() throws Exception {
        jwtVerifier.result = JwtVerificationResult.expired();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATTR_EXPIRED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("缺 token → 不設認證")
    void missingToken_noAuthentication() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    static class FakeJwtVerifier implements JwtVerifier {
        JwtVerificationResult result = JwtVerificationResult.invalid();

        @Override
        public JwtVerificationResult verify(String token) {
            return result;
        }
    }
}
