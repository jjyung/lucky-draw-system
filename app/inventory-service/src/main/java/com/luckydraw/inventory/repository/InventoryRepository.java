package com.luckydraw.inventory.repository;

import com.luckydraw.inventory.model.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 庫存真相來源的 repository。
 * 條件更新（native）是「絕不超抽」的最終保證（ADR-006 / ADR-010）：
 * - 扣減：WHERE stock >= quantity（rowcount 為判定結果）
 * - 配置同步：WHERE stock + delta >= 0 AND last_config_version < :cv
 */
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByPrizeId(Long prizeId);

    /**
     * 真相扣減（inventory-commit，ADR-006）。rowcount=1 → 扣減成功；0 → 庫存不足 → 補償。
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE inventory SET stock = stock - :qty, version = version + 1, " +
            "updated_at = CURRENT_TIMESTAMP WHERE prize_id = :prizeId AND stock >= :qty",
            nativeQuery = true)
    int deductStock(@Param("prizeId") Long prizeId, @Param("qty") int qty);

    /**
     * 配置差值套用（prize-stock-configured，ADR-010）。delta = newQuantity - oldQuantity。
     * rowcount=0 → 新總量 < 已發放數（或版本已套用/亂序）→ 不套用。
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE inventory SET stock = stock + :delta, version = version + 1, " +
            "last_config_version = :cv, updated_at = CURRENT_TIMESTAMP " +
            "WHERE prize_id = :prizeId AND stock + :delta >= 0 AND last_config_version < :cv",
            nativeQuery = true)
    int applyStockDelta(@Param("prizeId") Long prizeId, @Param("delta") int delta, @Param("cv") int configVersion);
}
