package com.luckydraw.auth.model.entity;

import jakarta.persistence.*;

/**
 * 角色（SA auth §5.1 `roles`；DB auth-db.md `roles`）。
 * 兩級權限分級：ROLE_USER / ROLE_ADMIN（FR-AUTH-05）。
 */
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    protected RoleEntity() {
    }

    public RoleEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
