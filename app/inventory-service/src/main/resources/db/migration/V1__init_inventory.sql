-- =============================================================
-- Inventory DB — V1 init
-- Service: inventory-service (ADR-002 schema-per-service)
-- 來源：docs/db/inventory-db.md（DDL 真相，直接落成 migration）
-- 防超抽: ADR-006（條件更新為真相）  冪等: ADR-005/006  配置同步: ADR-010
-- 註：H2 MODE=PostgreSQL 用 TIMESTAMP WITH TIME ZONE（非 TIMESTAMPTZ 縮寫）
-- =============================================================

-- inventory：庫存真相來源（source of truth）
-- prize_id 為跨 DB 邏輯引用（指向 campaign.prizes.id），無跨 DB FK
CREATE TABLE inventory (
    id                  BIGSERIAL    PRIMARY KEY,
    prize_id            BIGINT       NOT NULL,
    stock               INT          NOT NULL,
    version             INT          NOT NULL DEFAULT 0,
    last_config_version INT          NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_inventory_prize_id UNIQUE (prize_id),
    CONSTRAINT chk_inventory_stock   CHECK (stock >= 0)
);

-- reservations：預留/扣減記錄（冪等鍵 draw_record_id，ADR-005/006）
-- 生命週期：RESERVED → COMMITTED（成功）/ REVERSED（補償 or 超時回收）
CREATE TABLE reservations (
    id             BIGSERIAL    PRIMARY KEY,
    draw_record_id BIGINT       NOT NULL,
    prize_id       BIGINT       NOT NULL,
    quantity       INT          NOT NULL DEFAULT 1,
    status         VARCHAR(16)  NOT NULL DEFAULT 'RESERVED',
    reserved_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    committed_at   TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT uq_reservations_draw_record_id UNIQUE (draw_record_id),
    CONSTRAINT chk_reservations_quantity      CHECK (quantity >= 1),
    CONSTRAINT chk_reservations_status        CHECK (status IN ('RESERVED', 'COMMITTED', 'REVERSED')),
    CONSTRAINT chk_reservations_committed_at  CHECK (
        (status = 'COMMITTED' AND committed_at IS NOT NULL) OR
        (status <> 'COMMITTED' AND committed_at IS NULL)
    )
);

-- 依獎品查預留（對帳/稽核）
CREATE INDEX idx_reservations_prize_id ON reservations (prize_id);
-- 帳目校對掃描：逾時未完成的 RESERVED（UC-3 超時回收）
CREATE INDEX idx_reservations_status_reserved_at ON reservations (status, reserved_at);
