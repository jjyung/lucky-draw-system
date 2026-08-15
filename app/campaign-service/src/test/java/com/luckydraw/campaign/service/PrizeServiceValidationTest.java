package com.luckydraw.campaign.service;

import com.luckydraw.campaign.error.ApiException;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PrizeService 驗證邏輯測試（單元測試規約：保護不變量，純邏輯無 DB）。
 * 不變量：機率總和=100%、機率∈[0,100]、至少一個 THANK_YOU（FR-CAMP-04/06）。
 */
class PrizeServiceValidationTest {

    private PrizeEntity prize(String type, String probability, int stock) {
        return new PrizeEntity(null, "p", PrizeEntity.Type.valueOf(type), new BigDecimal(probability), stock, 0);
    }

    private List<PrizeEntity> validPrizes() {
        return List.of(
                prize("PRIZE", "5.00", 1),
                prize("PRIZE", "15.00", 10),
                prize("PRIZE", "30.00", 100),
                prize("THANK_YOU", "50.00", 0));
    }

    @Test
    @DisplayName("合法配置（總和=100、含 THANK_YOU）通過")
    void validConfig_passes() {
        assertThatCode(() -> PrizeService.validate(validPrizes())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("機率總和 ≠ 100% → A0303（AC-CAMP-001）")
    void sumNotHundred_rejected() {
        List<PrizeEntity> prizes = List.of(
                prize("PRIZE", "30.00", 1),
                prize("THANK_YOU", "60.00", 0)); // 總和 90

        assertThatThrownBy(() -> PrizeService.validate(prizes))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("A0303");
    }

    @Test
    @DisplayName("機率越界 [0,100] → A0304（AC-CAMP-002）")
    void probabilityOutOfRange_rejected() {
        List<PrizeEntity> prizes = List.of(
                prize("PRIZE", "101.00", 1),
                prize("THANK_YOU", "-1.00", 0));

        assertThatThrownBy(() -> PrizeService.validate(prizes))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("A0304");
    }

    @Test
    @DisplayName("缺 THANK_YOU → A0305（AC-CAMP-003）")
    void missingThankYou_rejected() {
        List<PrizeEntity> prizes = List.of(
                prize("PRIZE", "50.00", 1),
                prize("PRIZE", "50.00", 1)); // 總和 100 但無 THANK_YOU

        assertThatThrownBy(() -> PrizeService.validate(prizes))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("A0305");
    }

    @Test
    @DisplayName("空清單 → 拒絕")
    void emptyList_rejected() {
        assertThatThrownBy(() -> PrizeService.validate(List.of()))
                .isInstanceOf(ApiException.class);
    }
}
