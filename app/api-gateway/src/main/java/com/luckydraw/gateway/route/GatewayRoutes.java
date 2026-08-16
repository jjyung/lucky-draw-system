package com.luckydraw.gateway.route;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Gateway 對外 surface 的路徑語意（ADR-009 / gateway-service.yaml）。
 * 對外路徑帶 /api/v1 前綴；轉發下游時由路由 StripPrefix=2 剝除。
 * 公開功能清單封閉（UC-4）：register / login / jwks / GET campaigns / GET campaign detail。
 */
public final class GatewayRoutes {

    private GatewayRoutes() {
    }

    /**
     * 公開功能（免憑證，UC-4）；其餘一律需憑證。
     */
    public static boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (method == HttpMethod.POST) {
            return path.equals("/api/v1/auth/register") || path.equals("/api/v1/auth/login");
        }
        if (method == HttpMethod.GET) {
            return path.equals("/api/v1/auth/.well-known/jwks.json")
                    || path.equals("/api/v1/campaigns")
                    || isCampaignDetail(path);
        }
        return false;
    }

    /**
     * 抽獎路徑（UC-3：需 Idempotency-Key 存在性檢查）。
     */
    public static boolean isDraw(ServerHttpRequest request) {
        return request.getMethod() == HttpMethod.POST
                && request.getPath().value().matches("/api/v1/campaigns/\\d+/draw");
    }

    private static boolean isCampaignDetail(String path) {
        // /api/v1/campaigns/{id}（單段）
        String[] segments = path.split("/");
        return segments.length == 5
                && "api".equals(segments[1])
                && "v1".equals(segments[2])
                && "campaigns".equals(segments[3]);
    }
}
