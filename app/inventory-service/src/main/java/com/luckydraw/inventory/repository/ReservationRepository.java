package com.luckydraw.inventory.repository;

import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    boolean existsByDrawRecordId(Long drawRecordId);

    java.util.Optional<ReservationEntity> findByDrawRecordId(Long drawRecordId);

    List<ReservationEntity> findByPrizeId(Long prizeId);

    /**
     * 帳目校對（UC-3/FR-INV-05）：掃描逾時仍未完成的 RESERVED。
     */
    List<ReservationEntity> findByStatusAndReservedAtBefore(ReservationStateEnum status, OffsetDateTime before);

    /**
     * 超時回收：RESERVED → REVERSED（單向終態）。
     */
    @Modifying
    @Query("UPDATE ReservationEntity r SET r.status = com.luckydraw.contracts.inventory.api.model.ReservationStateEnum.REVERSED " +
            "WHERE r.drawRecordId = :drawRecordId AND r.status = com.luckydraw.contracts.inventory.api.model.ReservationStateEnum.RESERVED")
    int markReversed(@Param("drawRecordId") Long drawRecordId);
}
