package com.luckydraw.inventory.model.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * 庫存真相來源（inventory table，ADR-006）。每個 PRIZE 獎品一列；THANK_YOU 無此列。
 * stock 為唯一真相；條件更新（WHERE stock > 0 / stock + delta >= 0）保證絕不為負。
 */
@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prize_id", nullable = false, unique = true)
    private Long prizeId;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "last_config_version", nullable = false)
    private Integer lastConfigVersion = 0;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    protected InventoryEntity() {
    }

    public InventoryEntity(Long prizeId, Integer stock, Integer lastConfigVersion) {
        this.prizeId = prizeId;
        this.stock = stock;
        this.lastConfigVersion = lastConfigVersion;
    }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getPrizeId() {
        return prizeId;
    }

    public Integer getStock() {
        return stock;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getLastConfigVersion() {
        return lastConfigVersion;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
