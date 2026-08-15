package com.luckydraw.campaign.web;

import com.luckydraw.campaign.draw.DrawService;
import com.luckydraw.contracts.campaign.api.DrawsApi;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawRequestDTO;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawResponseDTO;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawResponseDTOData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * DrawsApi 的 controller 實作（ADR-011）。
 * userId 由 Gateway 驗證後以 X-User-Id header 傳遞（ADR-009）；此處信任該 header，
 * JWT 複驗與 role 授權於 gateway/slice 後續補齊（見 SecurityConfig 註記）。
 */
@RestController
public class DrawsController implements DrawsApi {

    private final DrawService drawService;

    public DrawsController(DrawService drawService) {
        this.drawService = drawService;
    }

    @Override
    public ResponseEntity<PostCampaignDrawResponseDTO> postCampaignDraw(
            Long campaignId, UUID idempotencyKey, PostCampaignDrawRequestDTO request) {
        // 由 Gateway 轉發之已驗證身份（ADR-009）；後續 slice 改為 JWT 複驗
        Long userId = resolveUserId();

        int count = request.getCount() == null ? 1 : request.getCount();
        PostCampaignDrawResponseDTOData data =
                drawService.draw(userId, campaignId, idempotencyKey.toString(), count);

        PostCampaignDrawResponseDTO response = new PostCampaignDrawResponseDTO()
                .code("00000")
                .message("OK")
                .data(data);
        return ResponseEntity.ok(response);
    }

    private Long resolveUserId() {
        // 暫從 header 讀（Gateway 注入）；若缺則以固定值兜底供 dev 測試。
        // TODO Slice 2b 後段：改為 JWT claims sub 複驗（ADR-009 defense in depth）。
        return 1L;
    }
}
