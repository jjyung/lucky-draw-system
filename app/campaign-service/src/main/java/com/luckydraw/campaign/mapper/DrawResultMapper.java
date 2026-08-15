package com.luckydraw.campaign.mapper;

import com.luckydraw.campaign.model.entity.DrawRecordEntity;
import com.luckydraw.contracts.campaign.api.model.BatchDrawResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultType;
import com.luckydraw.contracts.campaign.api.model.PrizeSummaryResourceDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DrawRecordEntity → DrawResultResourceDTO 轉換。
 * 以手寫轉換（非 MapStruct），因涉及 payload_json 快照反序列化與 nullable prize 語意。
 */
@Component
public class DrawResultMapper {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public DrawResultMapper(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 將單筆抽獎結果轉 DTO。優先回傳 replay 快照（payloadJson）；無有效快照時組裝。
     */
    public DrawResultResourceDTO toResult(DrawRecordEntity record) {
        String payload = record.getPayloadJson();
        if (payload != null && !payload.isBlank() && !"{}".equals(payload)) {
            try {
                return objectMapper.readValue(payload, DrawResultResourceDTO.class);
            } catch (Exception ignored) {
                // fallback to assembly below
            }
        }
        return assemble(record);
    }

    public BatchDrawResourceDTO toBatch(List<DrawRecordEntity> records) {
        List<DrawResultResourceDTO> draws = records.stream().map(this::toResult).toList();
        return new BatchDrawResourceDTO().draws(draws);
    }

    /**
     * 從 entity 組裝 DTO（不讀 payload），供 DrawService 產生快照用。
     */
    public DrawResultResourceDTO assemble(DrawRecordEntity record) {
        DrawResultResourceDTO dto = new DrawResultResourceDTO()
                .drawRecordId(record.getId())
                .campaignId(record.getCampaign().getId())
                .resultType(DrawResultType.fromValue(record.getResultType().name()));

        if (record.getResultType() == DrawRecordEntity.ResultType.WIN && record.getPrize() != null) {
            PrizeSummaryResourceDTO prize = new PrizeSummaryResourceDTO()
                    .id(record.getPrize().getId())
                    .name(record.getPrize().getName())
                    .type(com.luckydraw.contracts.campaign.api.model.PrizeType
                            .fromValue(record.getPrize().getType().name()));
            dto.prize(prize);
        }
        return dto;
    }
}
