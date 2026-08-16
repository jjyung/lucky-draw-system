package com.luckydraw.auth.service;

import com.luckydraw.auth.model.entity.RoleEntity;
import com.luckydraw.auth.model.entity.UserEntity;
import com.luckydraw.auth.repository.RoleRepository;
import com.luckydraw.auth.repository.UserRepository;
import com.luckydraw.auth.error.ApiException;
import com.luckydraw.auth.jwt.JwtKeyProvider;
import com.luckydraw.auth.jwt.JwtService;
import com.luckydraw.common.security.TokenRegistry;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * AuthService 的行為測試（單元測試規約：保護不變量，不 mock DB）。
 * 不變量來源：SA auth UC-1/UC-2 的 business rule 與 AC-AUTH-001/002/006、token 白名單（ADR-009 修訂）。
 */
@DataJpaTest
class AuthServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private FakeTokenRegistry tokenRegistry;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtKeyProvider keyProvider = new JwtKeyProvider();
        keyProvider.init();
        JwtService jwtService = new JwtService(keyProvider);
        tokenRegistry = new FakeTokenRegistry();
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, tokenRegistry);

        // @DataJpaTest 會執行 Flyway migration（含 V2 seed：ROLE_USER/ROLE_ADMIN），
        // 每個測試方法獨立事務並 rollback，不需手動清資料。此處僅驗證 seed 就緒。
        assertThat(roleRepository.findByCode("ROLE_USER")).isPresent();
    }

    @Test
    @DisplayName("註冊：密碼以 BCrypt 雜湊儲存，非明文（FR-AUTH-06）")
    void register_storesBcryptHash_notPlaintext() {
        String rawPassword = "S3cure!Pass";
        UserEntity user = authService.register("alice", "alice@example.com", rawPassword);

        assertThat(user.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("註冊：預設角色 ROLE_USER")
    void register_assignsDefaultRoleUser() {
        UserEntity user = authService.register("alice", "alice@example.com", "S3cure!Pass");

        assertThat(user.getRoles()).extracting(RoleEntity::getCode).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("註冊：重複 username → 409/A0101（AC-AUTH-002）")
    void register_duplicateUsername_conflict() {
        authService.register("alice", "alice@example.com", "S3cure!Pass");

        assertThatThrownBy(() -> authService.register("alice", "other@example.com", "S3cure!Pass"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException a = (ApiException) ex;
                    assertThat(a.getCode()).isEqualTo("A0101");
                    assertThat(a.getStatus().value()).isEqualTo(409);
                });
    }

    @Test
    @DisplayName("註冊：重複 email → 409/A0102（AC-AUTH-002）")
    void register_duplicateEmail_conflict() {
        authService.register("alice", "alice@example.com", "S3cure!Pass");

        assertThatThrownBy(() -> authService.register("bob", "alice@example.com", "S3cure!Pass"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException a = (ApiException) ex;
                    assertThat(a.getCode()).isEqualTo("A0102");
                    assertThat(a.getStatus().value()).isEqualTo(409);
                });
    }

    @Test
    @DisplayName("登入：成功簽發 access token（AC-AUTH-004）")
    void login_success_returnsToken() {
        authService.register("alice", "alice@example.com", "S3cure!Pass");

        String token = authService.login("alice", "S3cure!Pass");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("登入：以 email 登入亦可（SA UC-2 Precondition）")
    void login_byEmail_succeeds() {
        authService.register("alice", "alice@example.com", "S3cure!Pass");

        String token = authService.login("alice@example.com", "S3cure!Pass");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("登入：密碼錯誤 → 401/A0201（AC-AUTH-005）")
    void login_wrongPassword_unauthorized() {
        authService.register("alice", "alice@example.com", "S3cure!Pass");

        assertThatThrownBy(() -> authService.login("alice", "WrongPass"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException a = (ApiException) ex;
                    assertThat(a.getCode()).isEqualTo("A0201");
                    assertThat(a.getStatus().value()).isEqualTo(401);
                });
    }

    @Test
    @DisplayName("登入：帳號不存在與密碼錯誤同碼 401/A0201，不洩漏存在性（AC-AUTH-006）")
    void login_unknownAccount_sameCodeAsWrongPassword() {
        ApiException unknown = catchThrowableOfType(
                () -> authService.login("ghost", "whatever"),
                ApiException.class);
        ApiException wrongPwd = catchThrowableOfType(
                () -> {
                    authService.register("alice", "alice@example.com", "S3cure!Pass");
                    authService.login("alice", "WrongPass");
                },
                ApiException.class);

        assertThat(unknown.getCode()).isEqualTo(wrongPwd.getCode()).isEqualTo("A0201");
        assertThat(unknown.getStatus()).isEqualTo(wrongPwd.getStatus());
        assertThat(unknown.getMessage()).isEqualTo(wrongPwd.getMessage());
    }

    @Test
    @DisplayName("登入註冊 jti 進白名單；登出後撤銷（ADR-009 修訂）")
    void login_registersJti_logout_revokes() throws Exception {
        authService.register("alice", "alice@example.com", "S3cure!Pass");
        String token = authService.login("alice", "S3cure!Pass");

        String jti = SignedJWT.parse(token).getJWTClaimsSet().getJWTID();
        assertThat(jti).isNotBlank();
        assertThat(tokenRegistry.isActive(jti)).isTrue();

        authService.logout(token);
        assertThat(tokenRegistry.isActive(jti)).isFalse();
    }

    /** in-memory fake（classical）：active 集合內的 jti 視為在白名單。 */
    static class FakeTokenRegistry implements TokenRegistry {
        private final Set<String> active = new HashSet<>();

        @Override
        public void register(String userId, String jti, long ttlSeconds) {
            active.add(jti);
        }

        @Override
        public void revoke(String userId, String jti) {
            active.remove(jti);
        }

        @Override
        public boolean isActive(String jti) {
            return active.contains(jti);
        }
    }
}
