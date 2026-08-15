package com.luckydraw.campaign.web;

import com.luckydraw.campaign.error.ErrorCodes;
import com.luckydraw.campaign.mapper.CampaignMapper;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.service.CampaignService;
import com.luckydraw.contracts.campaign.api.CampaignsApi;
import com.luckydraw.contracts.campaign.api.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * CampaignsApi 的 controller 實作（ADR-011）。只做 DTO 組裝與轉發。
 */
@RestController
public class CampaignsController implements CampaignsApi {

    private final CampaignService campaignService;
    private final CampaignMapper campaignMapper;

    public CampaignsController(CampaignService campaignService, CampaignMapper campaignMapper) {
        this.campaignService = campaignService;
        this.campaignMapper = campaignMapper;
    }

    @Override
    public ResponseEntity<GetCampaignsResponseDTO> getCampaigns() {
        GetCampaignsResponseDTO response = new GetCampaignsResponseDTO()
                .code("00000")
                .message("OK")
                .data(campaignMapper.toSummaryList(campaignService.listAll()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GetCampaignByIdResponseDTO> getCampaignById(Long campaignId) {
        CampaignEntity campaign = campaignService.getCampaign(campaignId);
        GetCampaignByIdResponseDTO response = new GetCampaignByIdResponseDTO()
                .code("00000")
                .message("OK")
                .data(campaignMapper.toDetail(campaign));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PostCampaignsResponseDTO> postCampaigns(PostCampaignsRequestDTO request) {
        CampaignEntity campaign = campaignService.create(
                request.getName(), request.getStartTime(), request.getEndTime(), request.getDrawLimit());
        PostCampaignsResponseDTO response = new PostCampaignsResponseDTO()
                .code("00000")
                .message("OK")
                .data(campaignMapper.toResource(campaign));
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<PutCampaignByIdResponseDTO> putCampaignById(Long campaignId, PutCampaignByIdRequestDTO request) {
        CampaignEntity campaign = campaignService.update(
                campaignId, request.getName(), request.getStartTime(), request.getEndTime(), request.getDrawLimit());
        PutCampaignByIdResponseDTO response = new PutCampaignByIdResponseDTO()
                .code("00000")
                .message("OK")
                .data(campaignMapper.toResource(campaign));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PatchCampaignStatusResponseDTO> patchCampaignStatus(
            Long campaignId, PatchCampaignStatusRequestDTO request) {
        CampaignEntity.Status target = toStatus(request.getStatus());
        CampaignEntity campaign = campaignService.transitionStatus(campaignId, target);
        PatchCampaignStatusResponseDTO response = new PatchCampaignStatusResponseDTO()
                .code("00000")
                .message("OK")
                .data(campaignMapper.toResource(campaign));
        return ResponseEntity.ok(response);
    }

    private CampaignEntity.Status toStatus(CampaignStatus status) {
        return CampaignEntity.Status.valueOf(status.getValue());
    }
}
