package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import com.luckydraw.inventory.model.entity.ReservationEntity;
import com.luckydraw.inventory.redis.InventoryRedisClient;
import com.luckydraw.inventory.repository.InventoryRepository;
import com.luckydraw.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * inventory-commit 消費處理（UC-1 + UC-2，ADR-006/007）。
 * 單一 transaction 內：冪等 INSERT（draw_record_id UNIQUE 為併發兜底）→ 條件扣減 → COMMITTED / REVERSED。
 * 補償（庫存不足）：REVERSED + 加回 Redis 即時判定層 + 告警（NFR-06）。絕不負庫存。
 */
@Service
public class InventoryDeductionService {

    private static final Logger log = LoggerFactory.getLogger(InventoryDeductionService.class);

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryRedisClient redisClient;

    public InventoryDeductionService(ReservationRepository reservationRepository,
                                     InventoryRepository inventoryRepository,
                                     InventoryRedisClient redisClient) {
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.redisClient = redisClient;
    }

    @Transactional
    public void handleInventoryCommit(InventoryCommitEvent event) {
        Long drawRecordId = event.getDrawRecordId();
        Long prizeId = event.getPrizeId();
        int quantity = event.getQuantity() == null ? 1 : event.getQuantity();

        // 1. 冪等：已處理過 → 直接 ack，不重複扣減（ADR-007 at-least-once）
        if (reservationRepository.existsByDrawRecordId(drawRecordId)) {
            return;
        }

        // 2. INSERT reservation（RESERVED）。併發重複投遞撞 UNIQUE → 例外傳播 → binder retry 後走冪等分支。
        ReservationEntity reservation = new ReservationEntity(drawRecordId, prizeId, quantity);
        reservationRepository.saveAndFlush(reservation);

        // 3. 條件扣減（真相，WHERE stock >= quantity，絕不負庫存；clearAutomatically 後 reservation 已脫離）
        int updated = inventoryRepository.deductStock(prizeId, quantity);

        // 4. 重取 managed reservation，依扣減結果轉終態
        ReservationEntity managed = reservationRepository.findByDrawRecordId(drawRecordId).orElseThrow();
        if (updated == 1) {
            // 扣減成功 → COMMITTED（終態）
            managed.commit(OffsetDateTime.now());
        } else {
            // 庫存不足 → 補償（UC-2）：REVERSED + 加回即時判定層 + 告警
            managed.reverse();
            compensate(prizeId, drawRecordId, quantity);
        }
        reservationRepository.save(managed);
    }

    /**
     * 補償：加回 Redis 即時判定層額度（把誤扣的確認額度加回）+ 告警。
     * 註：Redis 的權威校正由帳目校對（UC-3）以 DB 為準收斂。
     */
    private void compensate(Long prizeId, Long drawRecordId, int quantity) {
        try {
            redisClient.incrementStock(prizeId, quantity);
        } catch (Exception e) {
            log.warn("補償時加回 Redis 即時判定層失敗（將由帳目校對收斂）prizeId={}, drawRecordId={}", prizeId, drawRecordId, e);
        }
        log.warn("庫存不足補償：撤銷中獎 drawRecordId={}, prizeId={}, quantity={}（FR-INV-03 / NFR-06）",
                drawRecordId, prizeId, quantity);
    }
}
