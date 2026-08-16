package com.luckydraw.campaign.security;

import com.luckydraw.common.security.JwtPrincipal;
import com.luckydraw.common.security.JwtVerificationResult;
import com.luckydraw.common.security.JwtVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 獨立複驗 filter（ADR-009 defense in depth）：不信任 Gateway 傳遞的 X-User-Id，
 * 以自行複驗的 claims（sub/roles）建立 SecurityContext。無效/過期則不設認證，
 * 由 entry point 回 401。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_EXPIRED = "jwt.expired";

    private final JwtVerifier jwtVerifier;

    public JwtAuthenticationFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            JwtVerificationResult result = jwtVerifier.verify(header.substring(7));
            if (result.isValid()) {
                setAuthentication(request, result.principal());
            } else if (result.status() == JwtVerificationResult.Status.EXPIRED) {
                request.setAttribute(ATTR_EXPIRED, Boolean.TRUE);
            }
        }
        chain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, JwtPrincipal principal) {
        List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal.userId(), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
