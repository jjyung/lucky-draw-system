package com.luckydraw.gateway.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.*;

/**
 * GatewayRoutes 路徑語意測試（UC-3/UC-4）：公開功能清單封閉、draw 路徑匹配。
 */
class GatewayRoutesTest {

    @Test
    @DisplayName("公開功能：register/login/jwks/GET campaigns/campaign detail")
    void publicPaths_recognized() {
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.post("/api/v1/auth/register").build())).isTrue();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.post("/api/v1/auth/login").build())).isTrue();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.get("/api/v1/auth/.well-known/jwks.json").build())).isTrue();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.get("/api/v1/campaigns").build())).isTrue();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.get("/api/v1/campaigns/5").build())).isTrue();
    }

    @Test
    @DisplayName("受保護功能：管理端點 / draw 皆非公開")
    void protectedPaths_notPublic() {
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.post("/api/v1/campaigns").build())).isFalse();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.put("/api/v1/campaigns/5").build())).isFalse();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.patch("/api/v1/campaigns/5/status").build())).isFalse();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.put("/api/v1/campaigns/5/prizes").build())).isFalse();
        assertThat(GatewayRoutes.isPublic(MockServerHttpRequest.post("/api/v1/campaigns/5/draw").build())).isFalse();
    }

    @Test
    @DisplayName("draw 路徑：僅 POST /campaigns/{id}/draw 命中")
    void drawPath_matchesOnlyDraw() {
        assertThat(GatewayRoutes.isDraw(MockServerHttpRequest.post("/api/v1/campaigns/5/draw").build())).isTrue();
        assertThat(GatewayRoutes.isDraw(MockServerHttpRequest.get("/api/v1/campaigns/5/draw").build())).isFalse();
        assertThat(GatewayRoutes.isDraw(MockServerHttpRequest.post("/api/v1/campaigns/5/status").build())).isFalse();
        assertThat(GatewayRoutes.isDraw(MockServerHttpRequest.post("/api/v1/campaigns").build())).isFalse();
    }
}
