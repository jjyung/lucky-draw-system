package com.luckydraw.campaign.web;

import com.luckydraw.campaign.mapper.CampaignMapper;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.campaign.service.CampaignService;
import com.luckydraw.campaign.service.PrizeService;
import com.luckydraw.contracts.campaign.api.PrizesApi;
import com.luckydraw.contracts.campaign.api.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PrizesApi 的 controller 實作（ADR-011）。
 */
@RestController
public class PrizesController implements PrizesApi {

    private final PrizeService prizeService;
    private final CampaignService campaignService;
    private final CampaignMapper campaignMapper;

    public PrizesController(PrizeService prizeService, CampaignService campaignService, CampaignMapper campaignMapper) {
        this.prizeService = prizeService;
        this.campaignService = campaignService;
        this.campaignMapper = campaignMapper;
    }

    @Override
    public ResponseEntity<PutCampaignPrizesResponseDTO> putCampaignPrizes(
            Long campaignId, PutCampaignPrizesRequestDTO request) {
        CampaignEntity campaign = campaignService.getCampaign(campaignId);

        List<PrizeEntity> prizes = toEntities(campaign, request.getPrizes());
        List<PrizeEntity> configured = prizeService.configure(campaignId, prizes);

        PrizesConfigResourceDTO config = new PrizesConfigResourceDTO()
                .campaignId(campaignId)
                .prizes(campaignMapper.toPrizeResourceList(configured));

        PutCampaignPrizesResponseDTO response = new PutCampaignPrizesResponseDTO()
                .code("00000")
                .message("OK")
                .data(config);
        return ResponseEntity.ok(response);
    }

    private List<PrizeEntity> toEntities(CampaignEntity campaign, List<PrizeInputDTO> inputs) {
        List<PrizeEntity> prizes = new ArrayList<>();
        int order = 0;
        for (PrizeInputDTO input : inputs) {
            PrizeEntity.Type type = PrizeEntity.Type.valueOf(input.getType().getValue());
            int stock = (type == PrizeEntity.Type.THANK_YOU) ? 0 : input.getQuantity();
            prizes.add(new PrizeEntity(
                    campaign,
                    input.getName(),
                    type,
                    input.getProbability(),
                    stock,
                    order++));
        }
        return prizes;
    }
}
