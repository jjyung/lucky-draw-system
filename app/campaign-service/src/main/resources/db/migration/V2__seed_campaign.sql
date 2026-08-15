-- =============================================================
-- Campaign DB — V2 seed
-- 來源：docs/db/dml-seed.md §3
-- 範例活動與獎品（機率總和 = 100%）
-- 注意：id 不顯式指定，由 BIGSERIAL 自動產生，避免 dev(H2) IDENTITY 序列衝突（ADR-011）
-- =============================================================

INSERT INTO campaigns (name, status, start_time, end_time, draw_limit) VALUES
    ('2026 中秋轉盤抽獎', 'ACTIVE',
     '2026-08-15 00:00:00+00',
     '2026-09-15 23:59:59+00',
     10);

-- 3 個 PRIZE + 1 個 THANK_YOU（機率總和 5 + 15 + 30 + 50 = 100）
INSERT INTO prizes (campaign_id, name, type, probability, stock, sort_order)
SELECT c.id, p.name, p.type, p.probability, p.stock, p.sort_order
FROM campaigns c,
     (VALUES
        ('iPhone 16 Pro',      'PRIZE',     5.00,   1, 1),
        ('Apple Watch Series', 'PRIZE',    15.00,  10, 2),
        ('星巴克 $100 兌換券',  'PRIZE',    30.00, 100, 3),
        ('銘謝惠顧',           'THANK_YOU', 50.00,   0, 4)
     ) AS p(name, type, probability, stock, sort_order)
WHERE c.name = '2026 中秋轉盤抽獎';
