package com.luckydraw.campaign.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckydraw.campaign.error.ErrorCodes;
import com.luckydraw.campaign.mapper.DrawResultMapper;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.DrawRecordEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.campaign.repository.CampaignRepository;
import com.luckydraw.campaign.repository.DrawRecordRepository;
import com.luckydraw.contracts.campaign.api.model.BatchDrawResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultResourceDTO;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawResponseDTOData;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 抽獎編排（SA UC-4/UC-5；draw-flow.md 正常路徑）。
 * 順序：ACTIVE 檢查 → 冪等鎖 → replay（查整批）→ 次數檢查 → N 次抽選(權重+庫存降級)
 *      → 落庫(同 tx、含快照、seq) → 計次 → 發布事件 → 組裝回應。
 *
 * 不變量（單元測試規約 §1 保護對象）：
 * - 冪等/replay：同鍵重送不重抽/不重扣/不重計（FR-CAMP-13/14）
 * - 防超抽：庫存不足降級 THANK_YOU，不重抽（FR-CAMP-19）
 * - 次數上限：活動期間總額，超限 429，批次不足整批不執行（FR-CAMP-11/12/15）
 * - 批次：單一 key 對應整批，落 N 筆（seq 0..N-1），同 tx（FR-CAMP-08）
 */
@Service
public class DrawService {

    private final CampaignRepository campaignRepository;
    private final DrawRecordRepository drawRecordRepository;
    private final DrawRedisClient redisClient;
    private final DrawResultMapper drawResultMapper;
    private final ObjectMapper objectMapper;
    private final DrawEventPublisher eventPublisher;
    private final TransactionTemplate txTemplate;

    public DrawService(CampaignRepository campaignRepository,
                       DrawRecordRepository drawRecordRepository,
                       DrawRedisClient redisClient,
                       DrawResultMapper drawResultMapper,
                       ObjectMapper objectMapper,
                       DrawEventPublisher eventPublisher,
                       PlatformTransactionManager transactionManager) {
        this.campaignRepository = campaignRepository;
        this.drawRecordRepository = drawRecordRepository;
        this.redisClient = redisClient;
        this.drawResultMapper = drawResultMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    public PostCampaignDrawResponseDTOData draw(Long userId, Long campaignId, String idempotencyKey, int count) {
        // 1. ACTIVE 檢查
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(ErrorCodes::campaignNotFound);
        if (campaign.getStatus() != CampaignEntity.Status.ACTIVE) {
            throw ErrorCodes.campaignNotFound(); // 非 ACTIVE 亦 404（AC-CAMP-005）
        }

        // 2. 冪等鎖
        if (!redisClient.tryLock(userId, campaignId, idempotencyKey)) {
            throw ErrorCodes.idempotencyConflict();
        }

    try {
        // 3. replay 查詢（整批）
        List<DrawRecordEntity> existing = drawRecordRepository
                .findByUserIdAndCampaignIdAndIdempotencyKeyOrderBySeqAsc(
                        userId, campaignId, idempotencyKey);
        if (!existing.isEmpty()) {
            return replay(existing, count);
        }
        // 4. 落庫在獨立交易執行（draw() 不掛 @Transactional，無外層交易時此處開新交易；
        //    撞 UNIQUE 時該交易獨立 rollback、不污染外層，catch 可於新交易重查）
        return txTemplate.execute(status -> executeDrawInTx(userId, campaignId, idempotencyKey, count));
    } catch (DataIntegrityViolationException e) {
        // 5. 併發撞 UNIQUE（ADR-005 第二道防線：DB UNIQUE 為最終冪等保證）→ 重查已落庫記錄 → replay
        List<DrawRecordEntity> existing = drawRecordRepository
                .findByUserIdAndCampaignIdAndIdempotencyKeyOrderBySeqAsc(
                        userId, campaignId, idempotencyKey);
        if (existing.isEmpty()) {
            throw e; // 非冪等衝突（如其他 constraint）→ 原異常
        }
        return replay(existing, count);
    } finally {
        redisClient.unlock(userId, campaignId, idempotencyKey);
    }
}

/**
 * 於交易內重載活動並執行抽選落庫（TransactionTemplate 提供交易）。
 */
private PostCampaignDrawResponseDTOData executeDrawInTx(Long userId, Long campaignId,
                                                       String idempotencyKey, int count) {
    CampaignEntity fresh = campaignRepository.findById(campaignId)
            .orElseThrow(ErrorCodes::campaignNotFound);
    return executeDraw(userId, fresh, idempotencyKey, count);
}

    private PostCampaignDrawResponseDTOData replay(List<DrawRecordEntity> records, int count) {
        if (count == 1) {
            return drawResultMapper.toResult(records.get(0));
        }
        return drawResultMapper.toBatch(records);
    }

    private PostCampaignDrawResponseDTOData executeDraw(Long userId, CampaignEntity campaign,
                                                        String idempotencyKey, int count) {
        // 4. 次數檢查（活動期間總額）；批次不足整批不執行（AC-CAMP-010）
        long used = redisClient.getDrawCount(userId, campaign.getId());
        if (used + count > campaign.getDrawLimit()) {
            throw ErrorCodes.drawLimitExceeded();
        }

        // 5. N 次抽選 + 落庫（同 tx）
        List<PrizeEntity> prizes = campaign.getPrizes();
        List<DrawRecordEntity> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            PrizeEntity hit = WeightedDrawEngine.select(prizes, ThreadLocalRandom.current().nextDouble(100.0));
            records.add(resolveResult(userId, campaign, idempotencyKey, i, hit));
        }

        // 7. 計次（僅成功產生結果的請求）
        redisClient.incrementDrawCount(userId, campaign.getId(), count, ttlSeconds(campaign));

        // 8. 發布事件（每筆 WIN）
        for (DrawRecordEntity record : records) {
            if (record.getResultType() == DrawRecordEntity.ResultType.WIN) {
                eventPublisher.publishInventoryCommit(record);
            }
        }

        // 9. 組裝回應
        if (count == 1) {
            return drawResultMapper.toResult(records.get(0));
        }
        return drawResultMapper.toBatch(records);
    }

    /**
     * 抽選命中後確認庫存；不足則降級 THANK_YOU（不重抽）。落庫一筆（含 seq、payload 快照）。
     */
    private DrawRecordEntity resolveResult(Long userId, CampaignEntity campaign,
                                           String idempotencyKey, int seq, PrizeEntity hit) {
        DrawRecordEntity.ResultType resultType;
        PrizeEntity prize = null;

        if (hit.getType() == PrizeEntity.Type.PRIZE) {
            if (redisClient.preDeduct(hit.getId())) {
                resultType = DrawRecordEntity.ResultType.WIN;
                prize = hit;
            } else {
                resultType = DrawRecordEntity.ResultType.THANK_YOU; // 降級不重抽（ADR-006）
            }
        } else {
            resultType = DrawRecordEntity.ResultType.THANK_YOU;
        }

        DrawRecordEntity record = new DrawRecordEntity(
                userId, campaign, idempotencyKey, seq, resultType, prize, "{}");
        record = drawRecordRepository.save(record);

        // 回填 payload 快照（replay 逐位元一致來源，campaign-db.md §3.2）
        try {
            DrawResultResourceDTO snapshot = drawResultMapper.assemble(record);
            record.setPayloadJson(objectMapper.writeValueAsString(snapshot));
            record = drawRecordRepository.save(record);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize draw snapshot", e);
        }
        return record;
    }

    private long ttlSeconds(CampaignEntity campaign) {
        long seconds = java.time.Duration.between(
                java.time.OffsetDateTime.now(), campaign.getEndTime()).getSeconds();
        return Math.max(seconds, 60L);
    }
}
