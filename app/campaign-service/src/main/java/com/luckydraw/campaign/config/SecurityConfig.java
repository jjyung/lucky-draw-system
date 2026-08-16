package com.luckydraw.campaign.config;

import com.luckydraw.campaign.security.JwtAccessDeniedHandler;
import com.luckydraw.campaign.security.JwtAuthenticationEntryPoint;
import com.luckydraw.campaign.security.JwtAuthenticationFilter;
import com.luckydraw.common.security.JwtVerificationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * campaign-service 安全配置（ADR-009 defense in depth）：
 * - JWT 獨立複驗（JwtAuthenticationFilter，自 Redis 公鑰）
 * - 授權：GET campaigns/{id} 公開；管理端點 ROLE_ADMIN；draw ROLE_USER（授權由本服務判定）
 * - 未認證 401（A0202/A0203）、權限不足 403（A0400）→ envelope
 */
@Configuration
@EnableWebSecurity
@Import(JwtVerificationConfig.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   JwtAuthenticationEntryPoint entryPoint,
                                                   JwtAccessDeniedHandler deniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/campaigns", "/campaigns/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/campaigns/*/draw").hasRole("USER")
                        .requestMatchers("/campaigns", "/campaigns/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
