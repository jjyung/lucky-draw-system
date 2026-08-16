package com.luckydraw.inventory.messaging;

import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import com.luckydraw.contracts.inventory.api.model.PrizeStockConfiguredEvent;
import com.luckydraw.contracts.inventory.api.model.ReservationStateEnum;
import com.luckydraw.inventory.model.entity.InventoryEntity;
import com.luckydraw.inventory.repository.InventoryRepository;
import com.luckydraw.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
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
 * inventory 事件投遞整合測試（ADR-007 at-least-once + ADR-006/010）。
 * 以 Spring Cloud Stream test-binder（in-memory）驗證兩個 binding 的接線：
 * - inventoryCommit：condition 扣減 + 冪等重投（drawRecordId）
 * - prizeStockConfigured：庫存調整（補貨/縮減 delta）+ configVersion 冪等/排序
 * （真 RabbitMQ 的 broker 投遞語意屬 prod 驗證，此處聚焦 function binding + JSON 反序列化。）
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestChannelBinderConfiguration.class)
class InventoryEventDeliveryIT {

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

    @Test
    @DisplayName("inventory-commit 投遞 → consumer 條件扣減；重複投遞 → 冪等不重複扣減")
    void inventoryCommit_delivered_andIdempotent() throws Exception {
        Long prizeId = 200L;
        inventoryRepository.saveAndFlush(new InventoryEntity(prizeId, 1, 0));

        InventoryCommitEvent event = new InventoryCommitEvent(2001L, prizeId, 1);
        Message<InventoryCommitEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("contentType", "application/json")
                .build();

        // 1. 首次投遞 → 扣減成功 COMMITTED
        input.send(message, "inventory-commit");
        waitFor(() -> reservationRepository.existsByDrawRecordId(2001L));

        assertThat(stockOf(prizeId)).isZero();
        assertThat(reservationRepository.findByDrawRecordId(2001L).orElseThrow().getStatus())
                .isEqualTo(ReservationStateEnum.COMMITTED);

        // 2. 重複投遞（at-least-once）→ 冪等，不重複扣減、庫存仍 0
        input.send(message, "inventory-commit");
        Thread.sleep(1000);

        assertThat(stockOf(prizeId)).isZero();
        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("prize-stock-configured 投遞：首次建置 → 冪等重投 → 補貨/縮減 delta → 亂序跳過")
    void prizeStockConfigured_delivered_appliesDelta_idempotentAndOrdered() throws Exception {
        Long prizeId = 300L;

        // 1. 首次建置：old=0, new=50, cv=1 → INSERT stock=50
        publishStock(new PrizeStockConfiguredEvent(prizeId, 7L, 0, 50, 1));
        waitFor(() -> stockOf(prizeId) == 50);
        assertThat(lastConfigVersionOf(prizeId)).isEqualTo(1);

        // 2. 同 configVersion 重投 → 冪等 skip（stock 仍 50）
        publishStock(new PrizeStockConfiguredEvent(prizeId, 7L, 0, 50, 1));
        Thread.sleep(500);
        assertThat(stockOf(prizeId)).isEqualTo(50);

        // 3. 補貨（delta +30）：old=50, new=80, cv=2 → stock=80
        publishStock(new PrizeStockConfiguredEvent(prizeId, 7L, 50, 80, 2));
        waitFor(() -> stockOf(prizeId) == 80);

        // 4. 縮減（delta -50）：old=80, new=30, cv=3 → stock=30
        publishStock(new PrizeStockConfiguredEvent(prizeId, 7L, 80, 30, 3));
        waitFor(() -> stockOf(prizeId) == 30);

        // 5. 較低 configVersion（亂序）→ skip（stock 仍 30）
        publishStock(new PrizeStockConfiguredEvent(prizeId, 7L, 30, 10, 1));
        Thread.sleep(500);
        assertThat(stockOf(prizeId)).isEqualTo(30);
    }

    private void publishStock(PrizeStockConfiguredEvent event) {
        Message<PrizeStockConfiguredEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("contentType", "application/json")
                .build();
        input.send(message, "prize-stock-configured");
    }

    private int stockOf(Long prizeId) {
        return inventoryRepository.findByPrizeId(prizeId).map(InventoryEntity::getStock).orElse(-1);
    }

    private int lastConfigVersionOf(Long prizeId) {
        return inventoryRepository.findByPrizeId(prizeId).map(InventoryEntity::getLastConfigVersion).orElse(-1);
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
