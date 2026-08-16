package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.PrizeStockConfiguredEvent;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.*;

/**
 * prize-stock-configured 消費處理測試（ADR-010）：upsert、delta、configVersion 排序、下限 guard。
 * 註：prize_id 用 100L 起，避開 V2 seed 的 1/2/3。
 */
@DataJpaTest
class InventoryProvisioningServiceTest {

    private static final Long PRIZE_ID = 100L;

    @Autowired
    private InventoryRepository inventoryRepository;

    private InventoryProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new InventoryProvisioningService(inventoryRepository);
    }

    @Test
    @DisplayName("首次建置 → INSERT（stock = newQuantity、last_config_version = v）")
    void firstInsert_createsRow() {
        service.handlePrizeStockConfigured(new PrizeStockConfiguredEvent(PRIZE_ID, 7L, 0, 10, 1));

        InventoryEntity inv = inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow();
        assertThat(inv.getStock()).isEqualTo(10);
        assertThat(inv.getLastConfigVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("差值套用：delta = new - old 更新 stock")
    void applyDelta_updatesStock() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 10, 0));

        service.handlePrizeStockConfigured(new PrizeStockConfiguredEvent(PRIZE_ID, 7L, 10, 15, 1)); // delta +5

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("較低/相同 configVersion → 跳過（冪等/亂序）")
    void lowerConfigVersion_skipped() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 15, 3));

        service.handlePrizeStockConfigured(new PrizeStockConfiguredEvent(PRIZE_ID, 7L, 15, 5, 2)); // 2 < 3

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isEqualTo(15); // 不變
    }

    @Test
    @DisplayName("下限 guard：新總量 < 已發放數 → 拒絕（不 clamp、stock 不變）")
    void insufficient_rejectedNotClamped() {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 10, 0));

        service.handlePrizeStockConfigured(new PrizeStockConfiguredEvent(PRIZE_ID, 7L, 25, 10, 1)); // delta -15 → stock 變負被拒

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isEqualTo(10); // 不變
    }
}
