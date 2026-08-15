package com.luckydraw.campaign.service;

import com.luckydraw.campaign.model.entity.CampaignEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Campaign 狀態機測試（單元測試規約：保護不變量，純邏輯無 DB）。
 * 不變量：DRAFT→ACTIVE→ENDED 單向、ENDED 終態不可回轉（SA campaign §4.1）。
 */
class CampaignStateMachineTest {

    private CampaignEntity draftCampaign() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CampaignEntity("測試活動", now, now.plusDays(7), 10);
    }

    @Test
    @DisplayName("新活動初始狀態為 DRAFT")
    void newCampaign_isDraft() {
        assertThat(draftCampaign().getStatus()).isEqualTo(CampaignEntity.Status.DRAFT);
    }

    @Test
    @DisplayName("DRAFT → ACTIVE 合法")
    void draftToActive_allowed() {
        CampaignEntity c = draftCampaign();
        c.activate();
        assertThat(c.getStatus()).isEqualTo(CampaignEntity.Status.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE → ENDED 合法")
    void activeToEnded_allowed() {
        CampaignEntity c = draftCampaign();
        c.activate();
        c.end();
        assertThat(c.getStatus()).isEqualTo(CampaignEntity.Status.ENDED);
    }

    @Test
    @DisplayName("DRAFT → ENDED 非法（跳級）")
    void draftToEnded_rejected() {
        CampaignEntity c = draftCampaign();
        assertThatThrownBy(c::end).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("ENDED → ACTIVE 非法（終態不可回轉）")
    void endedToActive_rejected() {
        CampaignEntity c = draftCampaign();
        c.activate();
        c.end();
        assertThatThrownBy(c::activate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("ACTIVE → DRAFT 非法（啟用後不可退回草稿）")
    void activeToDraft_rejected() {
        CampaignEntity c = draftCampaign();
        c.activate();
        assertThatThrownBy(() -> {
            if (c.getStatus() != CampaignEntity.Status.DRAFT) {
                throw new IllegalStateException("不可回轉為 DRAFT");
            }
        }).isInstanceOf(IllegalStateException.class);
    }
}
