package com.luckydraw.inventory.messaging;

import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import com.luckydraw.contracts.inventory.api.model.PrizeStockConfiguredEvent;
import com.luckydraw.inventory.service.InventoryDeductionService;
import com.luckydraw.inventory.service.InventoryProvisioningService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Spring Cloud Stream functional consumers（ADR-007）。
 * binding 名稱對應 application.yml 的 spring.cloud.function.definition。
 * - inventoryCommit：消費 inventory-commit（ADR-006/007）
 * - prizeStockConfigured：消費 prize-stock-configured（ADR-010）
 */
@Configuration
public class InventoryEventConsumers {

    @Bean
    public Consumer<InventoryCommitEvent> inventoryCommit(InventoryDeductionService service) {
        return service::handleInventoryCommit;
    }

    @Bean
    public Consumer<PrizeStockConfiguredEvent> prizeStockConfigured(InventoryProvisioningService service) {
        return service::handlePrizeStockConfigured;
    }
}
