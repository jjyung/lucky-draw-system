package com.luckydraw.campaign.web;

import com.luckydraw.campaign.draw.DrawService;
import com.luckydraw.campaign.error.ErrorCodes;
import com.luckydraw.contracts.campaign.api.DrawsApi;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawRequestDTO;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawResponseDTO;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawResponseDTOData;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * DrawsApi 的 controller 實作（ADR-011）。
 * userId 以 JWT 獨立複驗（JwtAuthenticationFilter）後的 sub 為準（defense in depth），
 * 不信任 Gateway 傳遞的 X-User-Id header。
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

    /**
     * 以複驗後 JWT 的 sub 為權威身份（principal 即 userId，見 JwtAuthenticationFilter）。
     */
    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw ErrorCodes.credentialInvalid();
    }
}
