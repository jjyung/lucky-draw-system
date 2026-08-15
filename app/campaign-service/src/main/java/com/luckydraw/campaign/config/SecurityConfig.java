package com.luckydraw.campaign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * campaign-service 過渡安全配置。
 * 註：ADR-009 要求各服務獨立複驗 JWT + 以 roles 授權（ROLE_ADMIN 才可管理端點）。
 * Slice 2a 先聚焦管理面（活動/獎品 CRUD），JWT 複驗與 role 授權於 Slice 2b（抽獎面）一併實作；
 * 在此之前放行全部，僅保留 actuator 健康檢查。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
