package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.PrizeStockConfiguredEvent;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * prize-stock-configured 消費處理（ADR-010）：以 prize_id upsert、以 config_version 去重/排序。
 * delta = newQuantity - oldQuantity；首次建置 INSERT（stock = newQuantity）；下限 guard 封住「新總量 < 已發放數」。
 */
@Service
public class InventoryProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(InventoryProvisioningService.class);

    private final InventoryRepository inventoryRepository;

    public InventoryProvisioningService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void handlePrizeStockConfigured(PrizeStockConfiguredEvent event) {
        Long prizeId = event.getPrizeId();
        int newQuantity = event.getNewQuantity();
        int configVersion = event.getConfigVersion();

        Optional<InventoryEntity> existing = inventoryRepository.findByPrizeId(prizeId);
        if (existing.isEmpty()) {
            // 首次建置：INSERT（stock = newQuantity、last_config_version = configVersion）
            try {
                inventoryRepository.saveAndFlush(new InventoryEntity(prizeId, newQuantity, configVersion));
            } catch (DataIntegrityViolationException e) {
                // 併發首次建置撞 UNIQUE → 重試後走既有分支
                return;
            }
            return;
        }

        // 冪等/排序：僅 incomingConfigVersion > last_config_version 時套用；相同/較低版本跳過
        if (configVersion <= existing.get().getLastConfigVersion()) {
            return;
        }

        int delta = event.getNewQuantity() - event.getOldQuantity();
        int updated = inventoryRepository.applyStockDelta(prizeId, delta, configVersion);
        if (updated == 0) {
            // 新總量 < 已發放數 → 拒絕（不 clamp）：stock 不變 + 記錄衝突 + 告警，由對帳收斂
            log.error("prize-stock-configured 拒絕套用（新總量低於已發放數）：prizeId={}, oldQuantity={}, newQuantity={}, configVersion={}",
                    prizeId, event.getOldQuantity(), newQuantity, configVersion);
        }
    }
}
