package com.luckydraw.inventory.messaging;

import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.repository.InventoryRepository;
import com.luckydraw.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.*;

/**
 * inventory-commit 事件投遞整合測試（ADR-007 at-least-once + ADR-006 冪等）。
 * 以 Spring Cloud Stream test-binder（in-memory）驗證 binding 接線：
 * 發布 InventoryCommitEvent → consumer 處理 → 條件扣減；重複投遞 → 冪等不重複扣減。
 * （真 RabbitMQ 的 broker 投遞語意屬 prod 驗證，此處聚焦 function binding + JSON 反序列化。）
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestChannelBinderConfiguration.class)
class InventoryCommitDeliveryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("luckydraw")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private InputDestination input;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final Long PRIZE_ID = 200L;

    @Test
    @DisplayName("inventory-commit 投遞 → consumer 條件扣減；重複投遞 → 冪等不重複扣減")
    void inventoryCommit_delivered_andIdempotent() throws Exception {
        inventoryRepository.saveAndFlush(new InventoryEntity(PRIZE_ID, 1, 0));

        InventoryCommitEvent event = new InventoryCommitEvent(2001L, PRIZE_ID, 1);
        Message<InventoryCommitEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("contentType", "application/json")
                .build();

        // 1. 首次投遞 → 扣減成功 COMMITTED
        input.send(message);
        waitFor(() -> reservationRepository.existsByDrawRecordId(2001L));

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isZero();
        assertThat(reservationRepository.findByDrawRecordId(2001L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.COMMITTED);

        // 2. 重複投遞（at-least-once）→ 冪等，不重複扣減、庫存仍 0
        input.send(message);
        Thread.sleep(1000);

        assertThat(inventoryRepository.findByPrizeId(PRIZE_ID).orElseThrow().getStock()).isZero();
        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    private void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        fail("等待 consumer 處理逾時");
    }
}
