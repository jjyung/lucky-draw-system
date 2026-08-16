package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.redis.InventoryRedisClient;
import com.luckydraw.inventory.repository.InventoryRepository;
import com.luckydraw.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * inventory-commit 消費處理測試（保護「絕不超抽」與「冪等」不變量，AC-INV-001~003）。
 * Redis 以 in-memory fake 替代（classical），DB 用真 H2（@DataJpaTest）。
 * 註：prize_id 用 100L 起，避開 V2 seed 的 1/2/3。
 */
@DataJpaTest
class InventoryDeductionServiceTest {

    private static final Long PRIZE_ID = 100L;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private FakeRedis redis;
    private InventoryDeductionService service;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        service = new InventoryDeductionService(reservationRepository, inventoryRepository, redis);
    }

    @Test
    @DisplayName("扣減成功 → stock 扣減、reservation COMMITTED（AC-INV-001）")
    void deductSuccess_commitsAndReducesStock() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 1, 0));

        service.handleInventoryCommit(new InventoryCommitEvent(1001L, PRIZE_ID, 1));

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isZero();
        assertThat(reservationRepository.findByDrawRecordId(1001L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.COMMITTED);
    }

    @Test
    @DisplayName("庫存不足 → 不負庫存、REVERSED + 加回 Redis（AC-INV-002）")
    void deductInsufficient_reversesAndCompensates() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 0, 0)); // 庫存 0

        service.handleInventoryCommit(new InventoryCommitEvent(1001L, PRIZE_ID, 1));

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isZero(); // 絕不為負
        assertThat(reservationRepository.findByDrawRecordId(1001L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.REVERSED);
        assertThat(redis.stock(PRIZE_ID)).isEqualTo(1); // 補償加回
    }

    @Test
    @DisplayName("冪等：同 drawRecordId 重複投遞只扣一次（AC-INV-003）")
    void idempotent_sameDrawRecordId_deductsOnce() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 1, 0));

        service.handleInventoryCommit(new InventoryCommitEvent(1001L, PRIZE_ID, 1));
        service.handleInventoryCommit(new InventoryCommitEvent(1001L, PRIZE_ID, 1)); // 重複投遞

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isZero(); // 非 -1
        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("絕不超抽：庫存 1，兩筆不同 drawRecordId → 一 COMMITTED 一 REVERSED")
    void noOversell_twoEvents_oneCommitsOneReverses() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 1, 0));

        service.handleInventoryCommit(new InventoryCommitEvent(1001L, PRIZE_ID, 1));
        service.handleInventoryCommit(new InventoryCommitEvent(1002L, PRIZE_ID, 1));

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isZero();
        assertThat(reservationRepository.findByDrawRecordId(1001L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.COMMITTED);
        assertThat(reservationRepository.findByDrawRecordId(1002L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.REVERSED);
    }

    static class FakeRedis implements InventoryRedisClient {
        private final Map<Long, Integer> stocks = new HashMap<>();

        @Override
        public void incrementStock(Long prizeId, int quantity) {
            stocks.merge(prizeId, quantity, Integer::sum);
        }

        @Override
        public void setStock(Long prizeId, int stock) {
            stocks.put(prizeId, stock);
        }

        int stock(Long prizeId) {
            return stocks.getOrDefault(prizeId, 0);
        }
    }
}
