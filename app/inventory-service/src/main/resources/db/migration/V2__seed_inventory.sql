-- =============================================================
-- Inventory DB — V2 seed
-- 來源：docs/db/dml-seed.md §4
-- 庫存初始值對齊 campaign 種子獎品（prize_id 1/2/3 = iPhone/Watch/星巴克）。
-- THANK_YOU（prize_id=4）無 inventory 列（銘謝惠顧不扣庫存）。
-- 註：id 不顯式指定，由 BIGSERIAL 自動產生（ADR-011）。
-- 此後一切扣減以 inventory-service 為準；Redis 即時判定層由帳目校對（initialDelay=0）以 DB 種子。
-- =============================================================

INSERT INTO inventory (prize_id, stock, version, last_config_version) VALUES
    (1,   1, 0, 0),   -- iPhone 16 Pro：剩餘 1（真相來源，可被條件更新扣減）
    (2,  10, 0, 0),   -- Apple Watch：剩餘 10
    (3, 100, 0, 0);   -- 星巴克券：剩餘 100
