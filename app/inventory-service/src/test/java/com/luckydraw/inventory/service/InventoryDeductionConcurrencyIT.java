package com.luckydraw.inventory.service;

import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.redis.InventoryRedisClient;
import com.luckydraw.inventory.repository.InventoryRepository;
import com.luckydraw.inventory.repository.ReservationRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 防超抽併發整合測試（FR-INV-02，AC-INV-001）。
 * 以真 PostgreSQL（Testcontainers）驗證「條件更新 WHERE stock >= qty」在併發下絕不超扣：
 * 庫存 1、N 個不同 drawRecordId 同時扣減 → 恰一筆 COMMITTED、其餘 REVERSED、stock 不為負。
 * H2 無法驗證此不變量（ADR-002），故需真 Postgres。
 */
@Tag("integration")
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryDeductionConcurrencyIT {

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

    private static final Long PRIZE_ID = 100L;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private InventoryDeductionService service;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        service = new InventoryDeductionService(reservationRepository, inventoryRepository, new NoopRedis());
        tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    @DisplayName("並發扣減：庫存 1、20 個不同 drawRecordId → 恰一 COMMITTED、stock 絕不負")
    void concurrentDeduct_neverNegative() throws Exception {
        // seed（已提交）
        tx.executeWithoutResult(s -> inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 1, 0)));

        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long drawRecordId = 1001L + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                tx.executeWithoutResult(s ->
                        service.handleInventoryCommit(new InventoryCommitEvent(drawRecordId, PRIZE_ID, 1)));
            }));
        }
        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // 以全新已提交交易讀取最終狀態（避開主線程測試交易的 stale view）
        int finalStock = tx.execute(s -> inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock());
        long committed = tx.execute(s -> reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStateEnum.COMMITTED)
                .count());
        long reversed = tx.execute(s -> reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStateEnum.REVERSED)
                .count());

        assertThat(finalStock).isZero();        // 絕不負庫存
        assertThat(committed).isEqualTo(1);     // 恰一筆成功
        assertThat(reversed).isEqualTo(n - 1);  // 其餘撤銷
    }

    static class NoopRedis implements InventoryRedisClient {
        @Override
        public void incrementStock(Long prizeId, int quantity) {
        }

        @Override
        public void setStock(Long prizeId, int stock) {
        }
    }
}
