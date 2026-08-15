package com.luckydraw.auth.mapper;

import com.luckydraw.auth.model.entity.RoleEntity;
import com.luckydraw.auth.model.entity.UserEntity;
import com.luckydraw.contracts.auth.api.model.UserResourceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * UserEntity → UserResourceDTO 映射（ADR-012，unmappedTargetPolicy=ERROR）。
 * id/username/email 同名自動映射；roles（Set<RoleEntity> → List<RolesEnum>）以 default method 轉換。
 * 密碼雜湊（passwordHash）非 DTO 欄位，天然不映射——絕不外洩（FR-AUTH-06）。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserResourceDTO toResource(UserEntity user);

    default UserResourceDTO.RolesEnum toRolesEnum(RoleEntity role) {
        return UserResourceDTO.RolesEnum.fromValue(role.getCode());
    }
}
