package com.luckydraw.campaign.service;

import com.luckydraw.campaign.error.ErrorCodes;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 活動 CRUD 與狀態機（SA UC-1 / UC-3）。
 * 不變量：DRAFT→ACTIVE→ENDED 單向、ENDED 終態不可回轉、建立後初始 DRAFT。
 */
@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public CampaignEntity create(String name, OffsetDateTime startTime, OffsetDateTime endTime, Integer drawLimit) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw ErrorCodes.statusConflict("結束時間必須晚於開始時間");
        }
        CampaignEntity campaign = new CampaignEntity(name, startTime, endTime, drawLimit);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public CampaignEntity update(Long campaignId, String name, OffsetDateTime startTime,
                           OffsetDateTime endTime, Integer drawLimit) {
        CampaignEntity campaign = getCampaign(campaignId);
        if (campaign.isEnded()) {
            throw ErrorCodes.statusConflict("已結束的活動不可編輯");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw ErrorCodes.statusConflict("結束時間必須晚於開始時間");
        }
        campaign.update(name, startTime, endTime, drawLimit);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public CampaignEntity transitionStatus(Long campaignId, CampaignEntity.Status target) {
        CampaignEntity campaign = getCampaign(campaignId);
        switch (target) {
            case ACTIVE -> campaign.activate();
            case ENDED -> campaign.end();
            case DRAFT -> throw ErrorCodes.statusConflict("不可回轉為 DRAFT");
            default -> throw ErrorCodes.statusConflict("非法狀態轉移");
        }
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignEntity> listAll() {
        return campaignRepository.findAllByOrderByStartTimeAsc();
    }

    @Transactional(readOnly = true)
    public CampaignEntity getCampaign(Long campaignId) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(ErrorCodes::campaignNotFound);
        // 觸發 lazy load，供 controller 層 toDetail 在交易外讀取（避免 LazyInitializationException）
        campaign.getPrizes().size();
        return campaign;
    }
}
