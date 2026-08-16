package com.luckydraw.campaign.service;

import com.luckydraw.campaign.error.ErrorCodes;
import com.luckydraw.campaign.event.PrizeStockEventPublisher;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.campaign.repository.CampaignRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 獎品配置（SA UC-2）。核心不變量（單元測試規約 §1 保護對象）：
 * - 每獎品機率 ∈ [0,100]（FR-CAMP-06）
 * - 全體（含 THANK_YOU）機率總和 = 100%（浮點容差內，FR-CAMP-04）
 * - 至少一個 THANK_YOU（FR-CAMP-06）
 * 任一失敗 → 整筆配置不生效（422）。
 *
 * 就地 reconcile（ADR-010）：依 sort_order 對位更新既有獎品（保留 id/config_version）、
 * 新增者建立、消失者刪除；保 prize_id 穩定（inventory upsert 鍵）。stock 變更時發布
 * prize-stock-configured（THANK_YOU 不發布）。
 *
 * <p>限制（POC）：對位採 sort_order。天條 FR-CAMP-05 僅要求「修改既有獎品的名稱/數量/機率」
 * （同集合重配）；runtime 插入/刪除/重排獎品會造成身份 remap（由 configVersion + 對帳收斂）。
 * 此為已知限制，非本 slice 修正範圍。
 */
@Service
public class PrizeService {

    /** 浮點容差（ADR-004：abs(sum-100) ≤ 1e-6） */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    private final CampaignRepository campaignRepository;
    private final PrizeStockEventPublisher prizeStockEventPublisher;
    private final EntityManager entityManager;

    public PrizeService(CampaignRepository campaignRepository, PrizeStockEventPublisher prizeStockEventPublisher,
                        EntityManager entityManager) {
        this.campaignRepository = campaignRepository;
        this.prizeStockEventPublisher = prizeStockEventPublisher;
        this.entityManager = entityManager;
    }

    /**
     * 整批覆蓋配置獎品（就地 reconcile）。驗證通過才生效；失敗拋 ApiException，原配置不變。
     */
    @Transactional
    public List<PrizeEntity> configure(Long campaignId, List<PrizeEntity> prizes) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(ErrorCodes::campaignNotFound);
        if (campaign.isEnded()) {
            throw ErrorCodes.statusConflict("已結束的活動不可配置獎品");
        }
        validate(prizes);

        List<StockChange> changes = new ArrayList<>();
        List<PrizeEntity> reconciled = reconcile(campaign, prizes, changes);
        // 明確 persist 新獎品（IDENTITY 立即回填 id），再 flush；
        // 不依賴 collection cascade（其 persist 延遲到 commit，事件發布時 id 尚未回填）。
        for (PrizeEntity p : reconciled) {
            if (p.getId() == null) {
                entityManager.persist(p);
            }
        }
        entityManager.flush();
        for (StockChange c : changes) {
            prizeStockEventPublisher.publishPrizeStockConfigured(
                    c.prize().getId(), campaign.getId(), c.oldQuantity(), c.newQuantity(), c.configVersion());
        }
        return reconciled;
    }

    /**
     * 就地 reconcile：依 sort_order 對位更新；新增／消失處理；quantity 變更時收集事件。
     */
    private List<PrizeEntity> reconcile(CampaignEntity campaign, List<PrizeEntity> prizes, List<StockChange> changes) {
        Map<Integer, PrizeEntity> existingByOrder = new HashMap<>();
        for (PrizeEntity p : campaign.getPrizes()) {
            existingByOrder.put(p.getSortOrder(), p);
        }

        List<PrizeEntity> result = new ArrayList<>(prizes.size());
        for (PrizeEntity incoming : prizes) {
            PrizeEntity target = existingByOrder.get(incoming.getSortOrder());
            if (target == null) {
                // 新獎品：建立（config_version = 1），待 flush 後取得 id 再發布
                target = incoming;
                target.setConfigVersion(1);
                if (target.getType() == PrizeEntity.Type.PRIZE) {
                    changes.add(new StockChange(target, 0, target.getStock(), 1));
                }
            } else {
                int oldQuantity = target.getStock();
                target.update(incoming.getName(), incoming.getType(), incoming.getProbability(), incoming.getStock());
                if (target.getType() == PrizeEntity.Type.PRIZE && oldQuantity != target.getStock()) {
                    int cv = target.getConfigVersion() + 1;
                    target.setConfigVersion(cv);
                    changes.add(new StockChange(target, oldQuantity, target.getStock(), cv));
                }
            }
            result.add(target);
        }

        // 移除消失的獎品（orphanRemoval 刪除）；加入新獎品。
        // 注意：不可對 PersistentBag 呼叫 sort()（會破壞 cascade persist 追蹤、新獎品 id 不落庫）；
        // result 本就依 sortOrder 順序建立，回應直接回 result，不需重排 collection。
        Set<PrizeEntity> kept = new HashSet<>(result);
        campaign.getPrizes().removeIf(p -> !kept.contains(p));
        for (PrizeEntity r : result) {
            if (!campaign.getPrizes().contains(r)) {
                campaign.getPrizes().add(r);
            }
        }
        return result;
    }

    /** 待發布的庫存配置變更（獎品、oldQuantity、newQuantity、configVersion）。 */
    private record StockChange(PrizeEntity prize, int oldQuantity, int newQuantity, int configVersion) {
    }

    /**
     * 純驗證邏輯（不變量，供單元測試直接驗證）。
     */
    public static void validate(List<PrizeEntity> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            throw ErrorCodes.probabilitySumInvalid();
        }

        boolean hasThankYou = false;
        BigDecimal sum = BigDecimal.ZERO;
        for (PrizeEntity p : prizes) {
            BigDecimal prob = p.getProbability();
            if (prob == null || prob.compareTo(BigDecimal.ZERO) < 0 || prob.compareTo(new BigDecimal("100")) > 0) {
                throw ErrorCodes.probabilityOutOfRange();
            }
            sum = sum.add(prob);
            if (p.getType() == PrizeEntity.Type.THANK_YOU) {
                hasThankYou = true;
            }
        }

        if (!hasThankYou) {
            throw ErrorCodes.missingThankYou();
        }
        if (sum.subtract(new BigDecimal("100")).abs().compareTo(TOLERANCE) > 0) {
            throw ErrorCodes.probabilitySumInvalid();
        }
    }
}
