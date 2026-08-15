package com.luckydraw.campaign.draw;

import com.luckydraw.campaign.model.entity.PrizeEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * 權重隨機抽選引擎（ADR-004）。
 * 純函數：單一 random ∈ [0,100) 走累計機率區間命中獎品，無任何 DB/Redis 依賴。
 * 固定順序（依獎品清單順序）以保證結果穩定。
 */
public final class WeightedDrawEngine {

    private WeightedDrawEngine() {
    }

    /**
     * 依機率選取一個獎品。
     *
     * @param prizes 獎品清單（含 THANK_YOU），機率總和已驗證 = 100%
     * @param random [0,100) 的隨機值（由呼叫方注入，便於測試）
     * @return 命中的獎品
     * @throws IllegalArgumentException random 越界時
     */
    public static PrizeEntity select(List<PrizeEntity> prizes, double random) {
        if (random < 0.0 || random >= 100.0) {
            throw new IllegalArgumentException("random must be in [0, 100)");
        }
        double cumulative = 0.0;
        for (PrizeEntity prize : prizes) {
            cumulative += prize.getProbability().doubleValue();
            if (random < cumulative) {
                return prize;
            }
        }
        // 理論上不可達（機率總和已驗證 = 100），但以防浮點誤差回傳最後一項
        return prizes.get(prizes.size() - 1);
    }
}
