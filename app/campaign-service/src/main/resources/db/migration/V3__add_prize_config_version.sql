-- =============================================================
-- Campaign DB — V3 新增 prizes.config_version
-- 來源：docs/db/campaign-db.md（config_version，ADR-010）
-- 每獎品單調遞增的配置版本，作為 prize-stock-configured 事件的冪等/排序鍵
-- =============================================================

ALTER TABLE prizes ADD COLUMN config_version INT NOT NULL DEFAULT 0;
