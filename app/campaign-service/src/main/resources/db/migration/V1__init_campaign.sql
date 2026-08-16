-- =============================================================
-- Campaign DB — V1 init
-- Service: campaign-service (ADR-002 schema-per-service)
-- 來源：docs/db/campaign-db.md（DDL 真相，直接落成 migration）
-- 冪等: ADR-005  權重抽獎/機率: ADR-004  狀態機: FR-CAMP-01
-- =============================================================

-- campaigns：抽獎活動（狀態機 DRAFT → ACTIVE → ENDED，ENDED 終態）
CREATE TABLE campaigns (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time   TIMESTAMP WITH TIME ZONE NOT NULL,
    draw_limit INT          NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_campaigns_status     CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED')),
    CONSTRAINT chk_campaigns_draw_limit CHECK (draw_limit >= 1),
    CONSTRAINT chk_campaigns_time       CHECK (end_time > start_time)
);

-- prizes：獎品（含銘謝惠顧 THANK_YOU，ADR-004）
CREATE TABLE prizes (
    id          BIGSERIAL    PRIMARY KEY,
    campaign_id BIGINT       NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name        VARCHAR(128) NOT NULL,
    type        VARCHAR(16)  NOT NULL,
    probability NUMERIC(5,2) NOT NULL,
    stock       INT          NOT NULL DEFAULT 0,
    sort_order  INT          NOT NULL DEFAULT 0,

    CONSTRAINT chk_prizes_type        CHECK (type IN ('PRIZE', 'THANK_YOU')),
    CONSTRAINT chk_prizes_probability CHECK (probability >= 0 AND probability <= 100),
    CONSTRAINT chk_prizes_stock       CHECK (stock >= 0)
);

-- 抽獎權重區間排序 + 依活動取獎品清單
CREATE INDEX idx_prizes_campaign_sort ON prizes (campaign_id, sort_order);

-- draw_records：抽獎結果（冪等 + replay 快照，ADR-005）
CREATE TABLE draw_records (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    campaign_id     BIGINT       NOT NULL REFERENCES campaigns(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(36)  NOT NULL,
    seq             INT          NOT NULL DEFAULT 0,
    result_type     VARCHAR(16)  NOT NULL,
    prize_id        BIGINT       NULL REFERENCES prizes(id) ON DELETE RESTRICT,
    payload_json    TEXT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_draw_records_idem       UNIQUE (user_id, campaign_id, idempotency_key, seq),
    CONSTRAINT chk_draw_records_result    CHECK (result_type IN ('WIN', 'THANK_YOU')),
    CONSTRAINT chk_draw_records_prize     CHECK (
        (result_type = 'WIN'       AND prize_id IS NOT NULL) OR
        (result_type = 'THANK_YOU' AND prize_id IS NULL)
    )
);

-- 依活動查詢抽獎記錄（管理/稽核，時間倒序）
CREATE INDEX idx_draw_records_campaign_created ON draw_records (campaign_id, created_at DESC);
