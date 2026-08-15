-- =============================================================
-- Auth DB — V2 seed
-- 來源：docs/db/dml-seed.md §2
-- POC 以種子資料預先建立 ROLE_USER / ROLE_ADMIN 與 ADMIN 帳號
-- （不提供任何提權 API — SA UC-4）
--
-- 注意：id 不顯式指定，由 BIGSERIAL 自動產生，避免 dev(H2) 與 prod(PG)
-- 的 IDENTITY 序列在 seed 後未推進而與後續 insert 衝突（ADR-011 H2 非等價）。
-- =============================================================

INSERT INTO roles (code, name) VALUES
    ('ROLE_USER',  '一般使用者'),
    ('ROLE_ADMIN', '管理人員');

-- ADMIN 帳號（BCrypt 佔位符，實際值由部署時以環境變數/Secret 覆蓋）
-- 預設密碼 admin123，BCrypt hash 與 Spring Security BCryptPasswordEncoder 相容
INSERT INTO users (username, email, password_hash) VALUES
    ('admin', 'admin@example.com', '$2y$10$O0zmtqBM3kfb1kvHL2iwTOBBB7k/ezcyfpQTVVSS39fRGD3Z7aPHm');

-- ADMIN 帳號授與 ROLE_ADMIN（依 code 子查詢，避免硬編 id）
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ROLE_ADMIN';
