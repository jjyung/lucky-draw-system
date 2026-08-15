package com.luckydraw.campaign.mapper;

import com.luckydraw.campaign.model.entity.Campaign;
import com.luckydraw.campaign.model.entity.Prize;
import com.luckydraw.contracts.campaign.api.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Campaign/Prize → DTO 映射（ADR-012，unmappedTargetPolicy=ERROR）。
 * 列表/詳情 DTO 本就不含 drawLimit（管理欄位），故無需 ignore；
 * 管理端 CampaignResourceDTO 含完整欄位（含 drawLimit）。
 * entity enum Status → DTO enum CampaignStatus 由 toStatus 轉換。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CampaignMapper {

    CampaignSummaryResourceDTO toSummary(Campaign campaign);

    List<CampaignSummaryResourceDTO> toSummaryList(List<Campaign> campaigns);

    CampaignResourceDTO toResource(Campaign campaign);

    CampaignDetailResourceDTO toDetail(Campaign campaign);

    List<PrizeSummaryResourceDTO> toPrizeSummaryList(List<Prize> prizes);

    List<PrizeResourceDTO> toPrizeResourceList(List<Prize> prizes);

    default CampaignStatus toStatus(Campaign.Status status) {
        return CampaignStatus.fromValue(status.name());
    }

    default PrizeType toPrizeType(Prize.Type type) {
        return PrizeType.fromValue(type.name());
    }

    default PrizeSummaryResourceDTO toPrizeSummary(Prize prize) {
        return new PrizeSummaryResourceDTO()
                .id(prize.getId())
                .name(prize.getName())
                .type(toPrizeType(prize.getType()));
    }

    default PrizeResourceDTO toPrizeResource(Prize prize) {
        return new PrizeResourceDTO()
                .id(prize.getId())
                .name(prize.getName())
                .type(toPrizeType(prize.getType()))
                .probability(prize.getProbability())
                .quantity(prize.getStock());
    }
}
