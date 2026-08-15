package com.luckydraw.campaign.repository;

import com.luckydraw.campaign.model.entity.DrawRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrawRecordRepository extends JpaRepository<DrawRecordEntity, Long> {

    /**
     * replay 查詢：以 user + campaign + idempotencyKey 取整批（含批次 N 筆），依 seq 排序。
     */
    List<DrawRecordEntity> findByUserIdAndCampaignIdAndIdempotencyKeyOrderBySeqAsc(
            Long userId, Long campaignId, String idempotencyKey);

    long countByUserIdAndCampaignId(Long userId, Long campaignId);
}
