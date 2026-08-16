package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.model.entity.ReservationEntity;
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
 * 帳目校對測試（ST-INV-004，AC-INV-004/005）。
 * 不變量：以 DB 為準校正 Redis 即時判定層；逾時 RESERVED → REVERSED + 加回 Redis 額度。
 * Redis 以 in-memory fake 替代（classical），DB 用真 H2（@DataJpaTest）。
 */
@DataJpaTest
class InventoryReconciliationServiceTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private FakeRedis redis;
    private InventoryReconciliationService service;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        service = new InventoryReconciliationService(inventoryRepository, reservationRepository, redis);
        // 逾時基準設為負值（cutoff = now + 1s），讓剛建立的 RESERVED 也算逾時，測試可確定
        service.setReservationTimeoutSeconds(-1);
    }

    @Test
    @DisplayName("以 DB 校正 Redis 即時判定層（AC-INV-004）")
    void realignRedisToDb_setsRedisFromDb() {
        inventoryRepository.saveAndFlush(new InventoryEntity(100L, 5, 0));
        inventoryRepository.saveAndFlush(new InventoryEntity(200L, 10, 0));

        service.reconcile();

        assertThat(redis.stock(100L)).isEqualTo(5);
        assertThat(redis.stock(200L)).isEqualTo(10);
    }

    @Test
    @DisplayName("逾時 RESERVED → REVERSED + 加回 Redis 額度（AC-INV-005）")
    void reclaimOverdueReservations_reversesAndRefunds() {
        inventoryRepository.saveAndFlush(new InventoryEntity(100L, 5, 0));
        reservationRepository.saveAndFlush(new ReservationEntity(1001L, 100L, 1)); // RESERVED, reserved_at=now

        service.reconcile();

        assertThat(reservationRepository.findByDrawRecordId(1001L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.REVERSED);
        assertThat(redis.stock(100L)).isEqualTo(6); // 5（DB 校正）+ 1（回收加回）
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
