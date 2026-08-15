package com.luckydraw.campaign.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckydraw.campaign.mapper.DrawResultMapper;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.campaign.repository.CampaignRepository;
import com.luckydraw.campaign.repository.DrawRecordRepository;
import com.luckydraw.contracts.campaign.api.model.BatchDrawResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultResourceDTO;
import com.luckydraw.contracts.campaign.api.model.DrawResultTypeEnum;
import com.luckydraw.campaign.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * DrawService 行為測試（單元測試規約：保護不變量）。
 * Redis 以 in-memory fake 替代（classical：stub 給固定答案），DB 用真 H2（@DataJpaTest）。
 * 不變量：冪等/replay、庫存不足降級、次數上限、批次 seq。
 */
@DataJpaTest
class DrawServiceTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DrawRecordRepository drawRecordRepository;

    private ObjectMapper objectMapper;
    private DrawResultMapper drawResultMapper;
    private FakeRedis redis;
    private DrawService drawService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        drawResultMapper = new DrawResultMapper(objectMapper);
        redis = new FakeRedis();
        DrawEventPublisher publisher = record -> {
        };
        drawService = new DrawService(campaignRepository, drawRecordRepository,
                redis, drawResultMapper, objectMapper, publisher);
    }

    private CampaignEntity activeCampaign(int drawLimit) {
        OffsetDateTime now = OffsetDateTime.now();
        CampaignEntity campaign = new CampaignEntity("測試活動", now, now.plusDays(7), drawLimit);
        campaign.activate();
        // 獎品：iPhone 10%（stock 1）、THANK_YOU 90%
        campaign.replacePrizes(java.util.List.of(
                new PrizeEntity(campaign, "iPhone", PrizeEntity.Type.PRIZE, new BigDecimal("10.00"), 1, 0),
                new PrizeEntity(campaign, "銘謝惠顧", PrizeEntity.Type.THANK_YOU, new BigDecimal("90.00"), 0, 1)));
        return campaignRepository.save(campaign);
    }

    @Test
    @DisplayName("單次抽獎成功落庫一筆（seq=0）")
    void singleDraw_recordsOne() {
        CampaignEntity campaign = activeCampaign(10);
        redis.stock(prizeIdOf(campaign), 1);

        var data = drawService.draw(42L, campaign.getId(), "key-1", 1);

        assertThat(data).isInstanceOf(DrawResultResourceDTO.class);
        assertThat(drawRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("replay：同鍵重送不重抽、回傳相同結果、不新增記錄（AC-CAMP-012）")
    void replay_sameKey_returnsSameResult_noNewRecord() {
        CampaignEntity campaign = activeCampaign(10);
        redis.stock(prizeIdOf(campaign), 1);

        var first = drawService.draw(42L, campaign.getId(), "key-1", 1);
        var second = drawService.draw(42L, campaign.getId(), "key-1", 1);

        assertThat(((DrawResultResourceDTO) second).getDrawRecordId())
                .isEqualTo(((DrawResultResourceDTO) first).getDrawRecordId());
        assertThat(drawRecordRepository.count()).isEqualTo(1);
        assertThat(redis.drawCount(42L, campaign.getId())).isEqualTo(1); // 不重複計次
    }

    @Test
    @DisplayName("庫存不足 → 降級 THANK_YOU，不重抽（AC-CAMP-014）")
    void stockInsufficient_degradeToThankYou() {
        CampaignEntity campaign = activeCampaign(10);
        redis.stock(prizeIdOf(campaign), 0); // 庫存 0

        var data = (DrawResultResourceDTO) drawService.draw(42L, campaign.getId(), "key-1", 1);

        assertThat(data.getResultType()).isEqualTo(DrawResultTypeEnum.THANK_YOU);
        assertThat(data.getPrize()).isNull();
    }

    @Test
    @DisplayName("次數超限 → 429/A0306（AC-CAMP-006）")
    void drawLimitExceeded_throws429() {
        CampaignEntity campaign = activeCampaign(1);
        redis.stock(prizeIdOf(campaign), 10);
        redis.setDrawCount(42L, campaign.getId(), 1); // 已用滿

        assertThatThrownBy(() -> drawService.draw(42L, campaign.getId(), "key-1", 1))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("A0306");
    }

    @Test
    @DisplayName("批次 count=N 落 N 筆（seq 0..N-1）、計次 N（AC-CAMP-008/009）")
    void batchDraw_recordsN_incrementsN() {
        CampaignEntity campaign = activeCampaign(10);
        redis.stock(prizeIdOf(campaign), 100);

        var data = drawService.draw(42L, campaign.getId(), "batch-key", 3);

        assertThat(data).isInstanceOf(BatchDrawResourceDTO.class);
        assertThat(((BatchDrawResourceDTO) data).getDraws()).hasSize(3);
        assertThat(drawRecordRepository.count()).isEqualTo(3);
        assertThat(redis.drawCount(42L, campaign.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("批次 replay：同鍵重送回 N 筆、不重抽（AC-CAMP-013）")
    void batchReplay_sameKey_returnsBatch_noNewRecord() {
        CampaignEntity campaign = activeCampaign(10);
        redis.stock(prizeIdOf(campaign), 100);

        drawService.draw(42L, campaign.getId(), "batch-key", 3);
        var replay = drawService.draw(42L, campaign.getId(), "batch-key", 3);

        assertThat(((BatchDrawResourceDTO) replay).getDraws()).hasSize(3);
        assertThat(drawRecordRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("批次不足（剩餘 < N）→ 整批 429 不執行（AC-CAMP-010）")
    void batchInsufficient_throws429_noPartial() {
        CampaignEntity campaign = activeCampaign(5);
        redis.stock(prizeIdOf(campaign), 100);
        redis.setDrawCount(42L, campaign.getId(), 3); // 已用 3，剩 2

        assertThatThrownBy(() -> drawService.draw(42L, campaign.getId(), "batch-key", 3))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("A0306");
        assertThat(drawRecordRepository.count()).isEqualTo(0); // 不產生部分結果
    }

    private Long prizeIdOf(CampaignEntity campaign) {
        return campaign.getPrizes().stream()
                .filter(p -> p.getType() == PrizeEntity.Type.PRIZE)
                .findFirst().orElseThrow().getId();
    }

    /** in-memory fake Redis（classical stub，非 mock framework） */
    static class FakeRedis implements DrawRedisClient {
        private final Set<String> locks = new HashSet<>();
        private final Map<Long, Integer> stocks = new HashMap<>();
        private final Map<String, Long> drawCounts = new HashMap<>();

        void stock(Long prizeId, int value) {
            stocks.put(prizeId, value);
        }

        void setDrawCount(Long userId, Long campaignId, long value) {
            drawCounts.put(userId + ":" + campaignId, value);
        }

        long drawCount(Long userId, Long campaignId) {
            return drawCounts.getOrDefault(userId + ":" + campaignId, 0L);
        }

        @Override
        public boolean tryLock(Long userId, Long campaignId, String idempotencyKey) {
            String key = userId + ":" + campaignId + ":" + idempotencyKey;
            return locks.add(key);
        }

        @Override
        public void unlock(Long userId, Long campaignId, String idempotencyKey) {
            locks.remove(userId + ":" + campaignId + ":" + idempotencyKey);
        }

        @Override
        public boolean preDeduct(Long prizeId) {
            int s = stocks.getOrDefault(prizeId, 0);
            if (s > 0) {
                stocks.put(prizeId, s - 1);
                return true;
            }
            return false;
        }

        @Override
        public void incrementDrawCount(Long userId, Long campaignId, long by, long ttlSeconds) {
            drawCounts.merge(userId + ":" + campaignId, by, Long::sum);
        }

        @Override
        public long getDrawCount(Long userId, Long campaignId) {
            return drawCounts.getOrDefault(userId + ":" + campaignId, 0L);
        }
    }
}
