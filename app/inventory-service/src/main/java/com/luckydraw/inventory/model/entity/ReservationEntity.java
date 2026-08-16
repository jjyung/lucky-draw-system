package com.luckydraw.inventory.model.entity;

import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * 預留/扣減記錄（reservations table，ADR-005/006）。draw_record_id 為冪等鍵（UNIQUE）。
 * 生命週期：RESERVED → COMMITTED（成功）/ REVERSED（補償或超時回收），終態不可回轉。
 */
@Entity
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draw_record_id", nullable = false, unique = true)
    private Long drawRecordId;

    @Column(name = "prize_id", nullable = false)
    private Long prizeId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStateEnum status = ReservationStateEnum.RESERVED;

    @Column(name = "reserved_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime reservedAt;

    @Column(name = "committed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime committedAt;

    protected ReservationEntity() {
    }

    public ReservationEntity(Long drawRecordId, Long prizeId, Integer quantity) {
        this.drawRecordId = drawRecordId;
        this.prizeId = prizeId;
        this.quantity = quantity;
        this.status = ReservationStateEnum.RESERVED;
        this.reservedAt = OffsetDateTime.now();
    }

    public void commit(OffsetDateTime at) {
        this.status = ReservationStateEnum.COMMITTED;
        this.committedAt = at;
    }

    public void reverse() {
        this.status = ReservationStateEnum.REVERSED;
    }

    public Long getId() {
        return id;
    }

    public Long getDrawRecordId() {
        return drawRecordId;
    }

    public Long getPrizeId() {
        return prizeId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReservationStateEnum getStatus() {
        return status;
    }

    public OffsetDateTime getReservedAt() {
        return reservedAt;
    }

    public OffsetDateTime getCommittedAt() {
        return committedAt;
    }
}
