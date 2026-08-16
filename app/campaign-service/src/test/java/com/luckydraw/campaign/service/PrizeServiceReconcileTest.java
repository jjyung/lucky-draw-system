package com.luckydraw.campaign.service;

import com.luckydraw.campaign.event.PrizeStockEventPublisher;
import com.luckydraw.campaign.model.entity.CampaignEntity;
import com.luckydraw.campaign.model.entity.PrizeEntity;
import com.luckydraw.campaign.repository.CampaignRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PrizeService 就地 reconcile 測試（ADR-010）：保 prize_id 穩定、config_version 遞增、事件發布。
 * 註：就地對位採 sort_order；POC 假設「同集合重配」為主（插入/重排會 remap 身份，屬已知限制，由 configVersion + 對帳收斂）。
 */
@DataJpaTest
class PrizeServiceReconcileTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private EntityManager entityManager;

    private FakePublisher publisher;
    private PrizeService prizeService;

    @BeforeEach
    void setUp() {
        publisher = new FakePublisher();
        prizeService = new PrizeService(campaignRepository, publisher, entityManager);
    }

    private CampaignEntity seedCampaign() {
        CampaignEntity c = new CampaignEntity("活動", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), 10);
        c.replacePrizes(List.of(
                new PrizeEntity(c, "iPhone", PrizeEntity.Type.PRIZE, new BigDecimal("5.00"), 1, 0),
                new PrizeEntity(c, "銘謝惠顧", PrizeEntity.Type.THANK_YOU, new BigDecimal("95.00"), 0, 1)));
        return campaignRepository.saveAndFlush(c);
    }

    @Test
    @DisplayName("同集合重配（quantity 變更）→ 保 id、config_version 遞增、發布事件")
    void reconfigure_preservesPrizeId_bumpsVersion_publishes() {
        CampaignEntity c = seedCampaign();
        Long originalId = c.getPrizes().get(0).getId();

        List<PrizeEntity> incoming = List.of(
                new PrizeEntity(c, "iPhone 新名", PrizeEntity.Type.PRIZE, new BigDecimal("10.00"), 5, 0), // stock 1→5
                new PrizeEntity(c, "銘謝惠顧", PrizeEntity.Type.THANK_YOU, new BigDecimal("90.00"), 0, 1));

        List<PrizeEntity> result = prizeService.configure(c.getId(), incoming);

        assertThat(result.get(0).getId()).isEqualTo(originalId); // 就地更新保住 id
        assertThat(result.get(0).getStock()).isEqualTo(5);
        assertThat(result.get(0).getConfigVersion()).isEqualTo(1); // 0→1
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).prizeId()).isEqualTo(originalId);
        assertThat(publisher.events.get(0).oldQuantity()).isEqualTo(1);
        assertThat(publisher.events.get(0).newQuantity()).isEqualTo(5);
        assertThat(publisher.events.get(0).configVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("quantity 未變更 → 不發布事件（ADR-010 僅建立/修改 quantity 發布）")
    void unchangedQuantity_noEvent() {
        CampaignEntity c = seedCampaign();

        List<PrizeEntity> incoming = List.of(
                new PrizeEntity(c, "iPhone", PrizeEntity.Type.PRIZE, new BigDecimal("5.00"), 1, 0),
                new PrizeEntity(c, "銘謝惠顧", PrizeEntity.Type.THANK_YOU, new BigDecimal("95.00"), 0, 1));

        prizeService.configure(c.getId(), incoming);

        assertThat(publisher.events).isEmpty();
    }

    @Test
    @DisplayName("初始配置（全新獎品）→ PRIZE 各發布 oldQuantity=0、config_version=1；THANK_YOU 不發布")
    void initialConfig_allNewPrizes_published() {
        // 活動無既有獎品
        CampaignEntity c = campaignRepository.saveAndFlush(
                new CampaignEntity("活動", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), 10));

        List<PrizeEntity> incoming = List.of(
                new PrizeEntity(c, "iPhone", PrizeEntity.Type.PRIZE, new BigDecimal("5.00"), 1, 0),
                new PrizeEntity(c, "Apple Watch", PrizeEntity.Type.PRIZE, new BigDecimal("15.00"), 10, 1),
                new PrizeEntity(c, "銘謝惠顧", PrizeEntity.Type.THANK_YOU, new BigDecimal("80.00"), 0, 2));

        prizeService.configure(c.getId(), incoming);

        assertThat(publisher.events).hasSize(2); // 兩個 PRIZE；THANK_YOU 不發布
        assertThat(publisher.events.get(0).oldQuantity()).isZero();
        assertThat(publisher.events.get(0).newQuantity()).isEqualTo(1);
        assertThat(publisher.events.get(0).configVersion()).isEqualTo(1);
        assertThat(publisher.events.get(0).prizeId()).isNotNull(); // 新獎品已 persist、id 非 null（回歸：曾因 merge/sort 而 null）
        assertThat(publisher.events.get(1).newQuantity()).isEqualTo(10);
        assertThat(publisher.events.get(1).configVersion()).isEqualTo(1);
        assertThat(publisher.events.get(1).prizeId()).isNotNull();
    }

    static class FakePublisher implements PrizeStockEventPublisher {
        List<Event> events = new ArrayList<>();

        record Event(Long prizeId, Long campaignId, int oldQuantity, int newQuantity, int configVersion) {
        }

        @Override
        public void publishPrizeStockConfigured(Long prizeId, Long campaignId,
                                                int oldQuantity, int newQuantity, int configVersion) {
            events.add(new Event(prizeId, campaignId, oldQuantity, newQuantity, configVersion));
        }
    }
}
