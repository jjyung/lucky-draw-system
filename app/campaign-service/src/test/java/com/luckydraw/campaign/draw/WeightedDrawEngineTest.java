package com.luckydraw.campaign.draw;

import com.luckydraw.campaign.model.entity.PrizeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * WeightedDrawEngine 測試（單元測試規約：保護不變量，純邏輯無 DB/Redis）。
 * 不變量：累計機率區間正確（ADR-004），區間邊界值落在正確獎品。
 * 配置：p1=5%, p2=15%, p3=30%, THANK_YOU=50%（SA AC-CAMP-015）。
 */
class WeightedDrawEngineTest {

    private PrizeEntity prize(String name, String type, String probability) {
        return new PrizeEntity(null, name, PrizeEntity.Type.valueOf(type),
                new BigDecimal(probability), 0, 0);
    }

    private List<PrizeEntity> config() {
        return List.of(
                prize("iPhone", "PRIZE", "5.00"),
                prize("Watch", "PRIZE", "15.00"),
                prize("Coupon", "PRIZE", "30.00"),
                prize("銘謝惠顧", "THANK_YOU", "50.00"));
    }

    @Test
    @DisplayName("累計區間：r=0 命中第一個獎品")
    void select_r0_hitsFirst() {
        assertThat(WeightedDrawEngine.select(config(), 0.0).getName()).isEqualTo("iPhone");
    }

    @Test
    @DisplayName("累計區間：r=4.99 仍在第一個區間（[0,5)）")
    void select_rBelow5_hitsFirst() {
        assertThat(WeightedDrawEngine.select(config(), 4.99).getName()).isEqualTo("iPhone");
    }

    @Test
    @DisplayName("累計區間邊界：r=5.0 命中第二個獎品（區間 [5,20)）")
    void select_r5_hitsSecond() {
        assertThat(WeightedDrawEngine.select(config(), 5.0).getName()).isEqualTo("Watch");
    }

    @Test
    @DisplayName("累計區間：r=20.0 命中第三個（區間 [20,50)）")
    void select_r20_hitsThird() {
        assertThat(WeightedDrawEngine.select(config(), 20.0).getName()).isEqualTo("Coupon");
    }

    @Test
    @DisplayName("累計區間：r=50.0 命中 THANK_YOU（區間 [50,100)）")
    void select_r50_hitsThankYou() {
        assertThat(WeightedDrawEngine.select(config(), 50.0).getName()).isEqualTo("銘謝惠顧");
    }

    @Test
    @DisplayName("累計區間：r=99.99 命中 THANK_YOU")
    void select_r99_hitsThankYou() {
        assertThat(WeightedDrawEngine.select(config(), 99.99).getName()).isEqualTo("銘謝惠顧");
    }

    @Test
    @DisplayName("random 越界 → IllegalArgumentException")
    void select_outOfRange_throws() {
        assertThatThrownBy(() -> WeightedDrawEngine.select(config(), 100.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightedDrawEngine.select(config(), -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
