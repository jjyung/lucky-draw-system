package com.luckydraw.auth.web;

import com.luckydraw.auth.api.AuthApi;
import com.luckydraw.auth.api.model.*;
import com.luckydraw.auth.domain.Role;
import com.luckydraw.auth.domain.User;
import com.luckydraw.auth.jwt.JwtKeyProvider;
import com.luckydraw.auth.jwt.JwtService;
import com.luckydraw.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthApi 的 controller 實作（openapi-generator 僅生成 interface，此處手寫 implements，ADR-011）。
 * 只做 DTO 組裝與轉發；業務邏輯在 AuthService / JwtService。
 */
@RestController
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtKeyProvider keyProvider;

    public AuthController(AuthService authService, JwtService jwtService, JwtKeyProvider keyProvider) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.keyProvider = keyProvider;
    }

    @Override
    public ResponseEntity<PostAuthRegisterResponseDTO> postAuthRegister(PostAuthRegisterRequestDTO request) {
        User user = authService.register(request.getUsername(), request.getEmail(), request.getPassword());

        UserResourceDTO resource = new UserResourceDTO()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(Role::getCode)
                        .map(code -> UserResourceDTO.RolesEnum.fromValue(code))
                        .toList());

        PostAuthRegisterResponseDTO response = new PostAuthRegisterResponseDTO()
                .code("00000")
                .message("OK")
                .data(resource);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PostAuthLoginResponseDTO> postAuthLogin(PostAuthLoginRequestDTO request) {
        String accessToken = authService.login(request.getUsername(), request.getPassword());

        TokenResourceDTO token = new TokenResourceDTO()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn((int) keyProvider.getTtlSeconds());

        PostAuthLoginResponseDTO response = new PostAuthLoginResponseDTO()
                .code("00000")
                .message("OK")
                .data(token);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GetAuthJwksResponseDTO> getAuthJwks() {
        GetAuthJwksResponseDTO response = new GetAuthJwksResponseDTO()
                .code("00000")
                .message("OK")
                .data(jwtService.jwks());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PostAuthRefreshResponseDTO> postAuthRefresh(PostAuthRefreshRequestDTO request) {
        // refresh 為 Should（FR-AUTH-04），非 POC 必須；Slice 1 不實作，回 501。
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
