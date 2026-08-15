package com.luckydraw.campaign.mapper;

import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.contracts.campaign.api.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * CampaignEntity/PrizeEntity → DTO 映射（ADR-012，unmappedTargetPolicy=ERROR）。
 * 列表/詳情 DTO 本就不含 drawLimit（管理欄位），故無需 ignore；
 * 管理端 CampaignResourceDTO 含完整欄位（含 drawLimit）。
 * entity enum Status → DTO enum CampaignStatusEnum 由 toStatus 轉換。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CampaignMapper {

    CampaignSummaryResourceDTO toSummary(CampaignEntity campaign);

    List<CampaignSummaryResourceDTO> toSummaryList(List<CampaignEntity> campaigns);

    CampaignResourceDTO toResource(CampaignEntity campaign);

    CampaignDetailResourceDTO toDetail(CampaignEntity campaign);

    List<PrizeSummaryResourceDTO> toPrizeSummaryList(List<PrizeEntity> prizes);

    List<PrizeResourceDTO> toPrizeResourceList(List<PrizeEntity> prizes);

    default CampaignStatusEnum toStatus(CampaignEntity.Status status) {
        return CampaignStatusEnum.fromValue(status.name());
    }

    default PrizeTypeEnum toPrizeType(PrizeEntity.Type type) {
        return PrizeTypeEnum.fromValue(type.name());
    }

    default PrizeSummaryResourceDTO toPrizeSummary(PrizeEntity prize) {
        return new PrizeSummaryResourceDTO()
                .id(prize.getId())
                .name(prize.getName())
                .type(toPrizeType(prize.getType()));
    }

    default PrizeResourceDTO toPrizeResource(PrizeEntity prize) {
        return new PrizeResourceDTO()
                .id(prize.getId())
                .name(prize.getName())
                .type(toPrizeType(prize.getType()))
                .probability(prize.getProbability())
                .quantity(prize.getStock());
    }
}
