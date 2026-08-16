package com.luckydraw.campaign.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckydraw.contracts.campaign.api.model.ErrorEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未認證 → 401 envelope（FR-X-01）。憑證過期 A0202、無效/缺憑證 A0203。
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        boolean expired = Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.ATTR_EXPIRED));
        String code = expired ? "A0202" : "A0203";
        String message = expired ? "憑證過期" : "憑證無效";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorEnvelope().code(code).message(message).data(null)));
    }
}
