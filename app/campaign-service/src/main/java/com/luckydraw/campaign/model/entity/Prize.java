package com.luckydraw.campaign.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 獎品（SA campaign §5.2；DB campaign-db.md `prizes`）。
 * 銘謝惠顧建模為 type=THANK_YOU 的獎品（ADR-004），quantity 忽略（無限庫存）。
 */
@Entity
@Table(name = "prizes")
public class Prize {

    public enum Type {
        PRIZE, THANK_YOU
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal probability;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected Prize() {
    }

    public Prize(Campaign campaign, String name, Type type, BigDecimal probability, Integer stock, Integer sortOrder) {
        this.campaign = campaign;
        this.name = name;
        this.type = type;
        this.probability = probability;
        this.stock = stock;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public Integer getStock() {
        return stock;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
