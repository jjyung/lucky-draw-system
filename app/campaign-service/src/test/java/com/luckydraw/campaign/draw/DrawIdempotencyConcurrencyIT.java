package com.luckydraw.campaign.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckydraw.campaign.mapper.DrawResultMapper;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.campaign.repository.CampaignRepository;
import com.luckydraw.campaign.repository.DrawRecordRepository;
import com.luckydraw.contracts.campaign.api.model.DrawResultResourceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 防重複抽獎併發整合測試（FR-CAMP-13/14，AC-CAMP-012）。
 * 以真 PostgreSQL（Testcontainers）驗證：Redis 冪等鎖失效（假設鎖恆通過）時，
 * DB `UNIQUE(user_id, campaign_id, idempotency_key, seq)` 仍為最終防線——
 * 同 key 併發抽獎 → 恰一筆落庫、其餘 replay 相同結果（不重抽、不重複落庫）。
 */
@Tag("integration")
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DrawIdempotencyConcurrencyIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("luckydraw")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DrawRecordRepository drawRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private DrawService drawService;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        DrawResultMapper mapper = new DrawResultMapper(objectMapper);
        drawService = new DrawService(campaignRepository, drawRecordRepository,
                new AlwaysAcquireRedis(), mapper, objectMapper, record -> {
                }, transactionManager);
        tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    @DisplayName("同 key 併發抽獎（鎖失效時）→ 恰一筆落庫、其餘 replay 相同 drawRecordId")
    void concurrentSameKey_exactlyOneDraw_othersReplay() throws Exception {
        Long campaignId = tx.execute(s -> seedCampaign().getId()); // 已提交

        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1L;
                }
                var data = drawService.draw(42L, campaignId, "same-key", 1);
                return ((DrawResultResourceDTO) data).getDrawRecordId();
            }));
        }
        ready.await();
        start.countDown();
        Set<Long> drawRecordIds = new HashSet<>();
        for (Future<Long> f : futures) {
            drawRecordIds.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        long recordCount = tx.execute(s -> drawRecordRepository.count());
        assertThat(recordCount).isEqualTo(1);   // 恰一筆落庫
        assertThat(drawRecordIds).hasSize(1);   // 所有回應同一 drawRecordId（replay 不重抽）
    }

    private CampaignEntity seedCampaign() {
        OffsetDateTime now = OffsetDateTime.now();
        CampaignEntity campaign = new CampaignEntity("併發測試活動", now, now.plusDays(7), 10);
        campaign.activate();
        campaign.replacePrizes(List.of(
                new PrizeEntity(campaign, "iPhone", PrizeEntity.Type.PRIZE, new BigDecimal("5.00"), 100, 0),
                new PrizeEntity(campaign, "銘謝惠顧", PrizeEntity.Type.THANK_YOU, new BigDecimal("95.00"), 0, 1)));
        return campaignRepository.saveAndFlush(campaign);
    }

    /** 鎖恆通過（模擬 Redis 冪等鎖失效），迫使 DB UNIQUE 成為唯一防線。 */
    static class AlwaysAcquireRedis implements DrawRedisClient {
        @Override
        public boolean tryLock(Long userId, Long campaignId, String idempotencyKey) {
            return true;
        }

        @Override
        public void unlock(Long userId, Long campaignId, String idempotencyKey) {
        }

        @Override
        public boolean preDeduct(Long prizeId) {
            return true;
        }

        @Override
        public void incrementDrawCount(Long userId, Long campaignId, long by, long ttlSeconds) {
        }

        @Override
        public long getDrawCount(Long userId, Long campaignId) {
            return 0;
        }
    }
}
