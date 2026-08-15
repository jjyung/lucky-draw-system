package com.luckydraw.campaign.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 抽獎結果（SA campaign §5.3；DB campaign-db.md `draw_records`）。
 * 冪等鍵 UNIQUE(user_id, campaign_id, idempotency_key)（ADR-005）；
 * payloadJson 為 replay 快照（FR-CAMP-14 逐位元一致）。
 * THANK_YOU 時 prizeId = NULL（campaign-db.md §3.3）。
 */
@Entity
@Table(name = "draw_records", uniqueConstraints = {
        @UniqueConstraint(name = "uq_draw_records_idem",
                columnNames = {"user_id", "campaign_id", "idempotency_key"})
})
public class DrawRecordEntity {

    public enum ResultType {
        WIN, THANK_YOU
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private CampaignEntity campaign;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(nullable = false)
    private Integer seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 16)
    private ResultType resultType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id")
    private PrizeEntity prize;

    @Column(name = "payload_json", nullable = false, columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    protected DrawRecordEntity() {
    }

    public DrawRecordEntity(Long userId, CampaignEntity campaign, String idempotencyKey,
                            Integer seq, ResultType resultType, PrizeEntity prize, String payloadJson) {
        this.userId = userId;
        this.campaign = campaign;
        this.idempotencyKey = idempotencyKey;
        this.seq = seq;
        this.resultType = resultType;
        this.prize = prize;
        this.payloadJson = payloadJson;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public CampaignEntity getCampaign() {
        return campaign;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Integer getSeq() {
        return seq;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public PrizeEntity getPrize() {
        return prize;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * 回填 replay 快照（僅落庫後由 DrawService 呼叫一次，見 campaign-db.md §3.2 註記）。
     */
    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
