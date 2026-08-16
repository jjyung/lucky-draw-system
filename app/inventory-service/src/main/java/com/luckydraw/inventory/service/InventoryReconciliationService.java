package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.model.entity.ReservationEntity;
import com.luckydraw.inventory.redis.InventoryRedisClient;
import com.luckydraw.inventory.repository.InventoryRepository;
import com.luckydraw.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 帳目校對（UC-3，FR-INV-05，Should）：以出貨真相校正即時判定層 + 回收超時未完成的扣減。
 * 1. 以 Inventory DB 剩餘庫存為基準，校正 Redis stock:{prizeId}（Redis 資料遺失可由 DB 重建）。
 * 2. 掃描逾時 RESERVED → REVERSED + 加回 Redis 額度 + 告警。
 * 校正方向單一：即時判定層對齊真相，不改寫出貨真相。
 */
@Service
public class InventoryReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReconciliationService.class);

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryRedisClient redisClient;

    @Value("${inventory.reconciliation.reservation-timeout-seconds:300}")
    private long reservationTimeoutSeconds;

    public InventoryReconciliationService(InventoryRepository inventoryRepository,
                                          ReservationRepository reservationRepository,
                                          InventoryRedisClient redisClient) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.redisClient = redisClient;
    }

    /** package-private（測試用）：設定逾時判定基準秒數。 */
    void setReservationTimeoutSeconds(long seconds) {
        this.reservationTimeoutSeconds = seconds;
    }

    /**
     * 定期校對（啟動後立即一次以種子 Redis 即時判定層，此後每 5 分鐘）。
     */
    @Scheduled(fixedDelayString = "${inventory.reconciliation.interval-ms:300000}", initialDelay = 0)
    public void reconcile() {
        try {
            realignRedisToDb();
            reclaimOverdueReservations();
        } catch (Exception e) {
            log.error("帳目校對執行失敗", e);
        }
    }

    /**
     * 以 DB 為準校正 Redis 即時判定層（stock:{prizeId}）。
     */
    private void realignRedisToDb() {
        List<InventoryEntity> rows = inventoryRepository.findAll();
        int corrected = 0;
        for (InventoryEntity row : rows) {
            try {
                redisClient.setStock(row.getPrizeId(), row.getStock());
                corrected++;
            } catch (Exception e) {
                log.warn("校正 Redis 計數器失敗 prizeId={}", row.getPrizeId(), e);
            }
        }
        log.info("帳目校對：已以 DB 校正 Redis 即時判定層 {} 筆", corrected);
    }

    /**
     * 回收逾時未完成的 RESERVED（加回 Redis 額度 + 告警）。
     */
    private void reclaimOverdueReservations() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(reservationTimeoutSeconds);
        List<ReservationEntity> overdue = reservationRepository
                .findByStatusAndReservedAtBefore(ReservationStateEnum.RESERVED, cutoff);
        for (ReservationEntity r : overdue) {
            int updated = reservationRepository.markReversed(r.getDrawRecordId());
            if (updated > 0) {
                try {
                    redisClient.incrementStock(r.getPrizeId(), r.getQuantity());
                } catch (Exception e) {
                    log.warn("超時回收加回 Redis 失敗 prizeId={}", r.getPrizeId(), e);
                }
                log.warn("超時回收：reservation drawRecordId={}, prizeId={} 已 REVERSED（FR-INV-05）",
                        r.getDrawRecordId(), r.getPrizeId());
            }
        }
    }
}
