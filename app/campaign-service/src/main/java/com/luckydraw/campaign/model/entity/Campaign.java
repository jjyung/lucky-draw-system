package com.luckydraw.campaign.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽獎活動（SA campaign §5.1；DB campaign-db.md `campaigns`）。
 * 狀態機 DRAFT → ACTIVE → ENDED（單向，ENDED 終態不可回轉）。
 */
@Entity
@Table(name = "campaigns")
public class Campaign {

    public enum Status {
        DRAFT, ACTIVE, ENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.DRAFT;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;

    @Column(name = "draw_limit", nullable = false)
    private Integer drawLimit;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Prize> prizes = new ArrayList<>();

    protected Campaign() {
    }

    public Campaign(String name, OffsetDateTime startTime, OffsetDateTime endTime, Integer drawLimit) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.drawLimit = drawLimit;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- 狀態機語意（SA campaign §4.1） ---
    public void activate() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("僅 DRAFT 可啟用為 ACTIVE，目前 " + this.status);
        }
        this.status = Status.ACTIVE;
    }

    public void end() {
        if (this.status != Status.ACTIVE) {
            throw new IllegalStateException("僅 ACTIVE 可結束為 ENDED，目前 " + this.status);
        }
        this.status = Status.ENDED;
    }

    public boolean isEnded() {
        return this.status == Status.ENDED;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public Integer getDrawLimit() {
        return drawLimit;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Prize> getPrizes() {
        return prizes;
    }

    public void update(String name, OffsetDateTime startTime, OffsetDateTime endTime, Integer drawLimit) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.drawLimit = drawLimit;
    }

    public void replacePrizes(List<Prize> newPrizes) {
        this.prizes.clear();
        this.prizes.addAll(newPrizes);
    }
}
