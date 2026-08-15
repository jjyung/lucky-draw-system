package com.luckydraw.auth.mapper;

import com.luckydraw.auth.model.entity.RoleEntity;
import com.luckydraw.auth.model.entity.UserEntity;
import com.luckydraw.contracts.auth.api.model.UserResourceDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * UserMapper 行為測試（單元測試規約：保護不變量）。
 * 不變量：映射不遺漏欄位（編譯期 ERROR 已兜底）、密碼雜湊不進入 DTO（FR-AUTH-06）、
 * roles 正確轉換（Role.code → RolesEnum）。
 */
class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    @DisplayName("基本欄位映射正確")
    void toResource_mapsBasicFields() {
        UserEntity user = new UserEntity("alice", "alice@example.com", "$2y$hash");
        user.addRole(new RoleEntity("ROLE_USER", "一般使用者"));

        UserResourceDTO dto = userMapper.toResource(user);

        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("roles 正確轉換為 RolesEnum（Role.code → RolesEnum）")
    void toResource_mapsRoles() {
        UserEntity user = new UserEntity("alice", "alice@example.com", "$2y$hash");
        user.addRole(new RoleEntity("ROLE_USER", "一般使用者"));
        user.addRole(new RoleEntity("ROLE_ADMIN", "管理人員"));

        UserResourceDTO dto = userMapper.toResource(user);

        assertThat(dto.getRoles())
                .extracting(UserResourceDTO.RolesEnum::getValue)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("密碼雜湊絕不進入 DTO（DTO 無此欄位，FR-AUTH-06）")
    void toResource_neverExposesPasswordHash() {
        UserEntity user = new UserEntity("alice", "alice@example.com", "$2y$secret-hash");

        UserResourceDTO dto = userMapper.toResource(user);

        // UserResourceDTO 沒有 password/passwordHash 欄位，此斷言保證映射介面未外洩
        assertThat(dto.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("password"));
    }
}
