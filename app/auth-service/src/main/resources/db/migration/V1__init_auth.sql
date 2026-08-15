-- =============================================================
-- Auth DB — V1 init
-- Service: auth-service (ADR-002 schema-per-service)
-- 來源：docs/db/auth-db.md（DDL 真相，直接落成 migration）
-- 密碼安全: FR-AUTH-06 (BCrypt 不可逆)  權限分級: FR-AUTH-05
-- =============================================================

-- users：使用者帳號（密碼 BCrypt 雜湊，任何環節不得明文）
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- roles：角色（兩級權限分級）
CREATE TABLE roles (
    id   BIGSERIAL   PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,

    CONSTRAINT uq_roles_code UNIQUE (code)
);

-- user_roles：使用者-角色關聯（多對多）
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id)
);

-- 反向查詢：依角色找使用者（授權盤點/稽核）
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
