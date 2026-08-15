package com.luckydraw.campaign.service;

import com.luckydraw.campaign.error.ErrorCodes;
import com.luckydraw.campaign.model.entity.Campaign;
import com.luckydraw.campaign.model.entity.Prize;
import com.luckydraw.campaign.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 獎品配置（SA UC-2）。核心不變量（單元測試規約 §1 保護對象）：
 * - 每獎品機率 ∈ [0,100]（FR-CAMP-06）
 * - 全體（含 THANK_YOU）機率總和 = 100%（浮點容差內，FR-CAMP-04）
 * - 至少一個 THANK_YOU（FR-CAMP-06）
 * 任一失敗 → 整筆配置不生效（422）。
 */
@Service
public class PrizeService {

    /** 浮點容差（ADR-004：abs(sum-100) ≤ 1e-6） */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    private final CampaignRepository campaignRepository;

    public PrizeService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    /**
     * 整批覆蓋配置獎品。驗證通過才生效；失敗拋 ApiException，原配置不變。
     */
    @Transactional
    public List<Prize> configure(Long campaignId, List<Prize> prizes) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(ErrorCodes::campaignNotFound);
        if (campaign.isEnded()) {
            throw ErrorCodes.statusConflict("已結束的活動不可配置獎品");
        }
        validate(prizes);
        campaign.replacePrizes(prizes);
        campaignRepository.save(campaign);
        return campaign.getPrizes();
    }

    /**
     * 純驗證邏輯（不變量，供單元測試直接驗證）。
     */
    public static void validate(List<Prize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            throw ErrorCodes.probabilitySumInvalid();
        }

        boolean hasThankYou = false;
        BigDecimal sum = BigDecimal.ZERO;
        for (Prize p : prizes) {
            BigDecimal prob = p.getProbability();
            if (prob == null || prob.compareTo(BigDecimal.ZERO) < 0 || prob.compareTo(new BigDecimal("100")) > 0) {
                throw ErrorCodes.probabilityOutOfRange();
            }
            sum = sum.add(prob);
            if (p.getType() == Prize.Type.THANK_YOU) {
                hasThankYou = true;
            }
        }

        if (!hasThankYou) {
            throw ErrorCodes.missingThankYou();
        }
        if (sum.subtract(new BigDecimal("100")).abs().compareTo(TOLERANCE) > 0) {
            throw ErrorCodes.probabilitySumInvalid();
        }
    }
}
