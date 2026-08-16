package com.luckydraw.auth.config;

import com.luckydraw.common.security.JwtVerificationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auth-service 僅「簽發」憑證，不驗證（驗證由 Gateway 與各服務獨立複驗，ADR-009 defense in depth）。
 * 本服務端點皆為 PUBLIC（register/login/jwks/logout），故放行全部；refresh（Should）於後續 slice 再補
 * bearerAuth 驗證。密碼以 BCrypt 不可逆雜湊（FR-AUTH-06）。
 * 引入 JwtVerificationConfig 取得 TokenRegistry（登入白名單註冊 / 登出撤銷，ADR-009 修訂）。
 */
@Configuration
@EnableWebSecurity
@Import(JwtVerificationConfig.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
