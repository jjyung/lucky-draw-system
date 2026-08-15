package com.luckydraw.auth.service;

import com.luckydraw.auth.model.entity.RoleEntity;
import com.luckydraw.auth.model.entity.UserEntity;
import com.luckydraw.auth.repository.RoleRepository;
import com.luckydraw.auth.repository.UserRepository;
import com.luckydraw.auth.error.ErrorCodes;
import com.luckydraw.auth.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 註冊與登入的業務邏輯（SA UC-1 / UC-2）。
 * 不變量（單元測試規約 §1 保護對象）：
 * - 密碼以不可逆 BCrypt 雜湊儲存，任何環節不得明文（FR-AUTH-06）
 * - username/email 唯一，重複 → 409（AC-AUTH-002）
 * - 登入失敗「帳號不存在」與「密碼錯誤」回同一 401/A0201（AC-AUTH-006）
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 註冊（UC-1）。預設 ROLE_USER，回傳建立的使用者（不含密碼雜湊）。
     */
    @Transactional
    public UserEntity register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw ErrorCodes.usernameExists();
        }
        if (userRepository.existsByEmail(email)) {
            throw ErrorCodes.emailExists();
        }

        RoleEntity defaultRole = roleRepository.findByCode("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER 角色不存在（seed 資料缺失）"));

        UserEntity user = new UserEntity(username, email, passwordEncoder.encode(rawPassword));
        user.addRole(defaultRole);
        return userRepository.save(user);
    }

    /**
     * 登入（UC-2）。成功回傳簽發的 access token；失敗回傳統一 401（不洩漏存在性）。
     */
    @Transactional(readOnly = true)
    public String login(String usernameOrEmail, String rawPassword) {
        UserEntity user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElse(null);

        // 帳號不存在與密碼錯誤回同一碼，且避免時序側洩漏（皆執行比對路徑）
        if (user == null) {
            passwordEncoder.encode(rawPassword); // 消耗相近時間，減少帳號存在性側洩漏
            throw ErrorCodes.badCredentials();
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw ErrorCodes.badCredentials();
        }

        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getCode)
                .toList();
        return jwtService.issueAccessToken(user.getId(), roles);
    }
}
