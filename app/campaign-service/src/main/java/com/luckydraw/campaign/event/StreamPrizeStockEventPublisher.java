package com.luckydraw.campaign.event;

import com.luckydraw.contracts.inventory.api.model.PrizeStockConfiguredEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * PrizeStockEventPublisher 的 Spring Cloud Stream 實作（ADR-010）。
 * 發布 prize-stock-configured，binding 見 application.yml。
 */
@Component
public class StreamPrizeStockEventPublisher implements PrizeStockEventPublisher {

    private final StreamBridge streamBridge;

    public StreamPrizeStockEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publishPrizeStockConfigured(Long prizeId, Long campaignId,
                                            int oldQuantity, int newQuantity, int configVersion) {
        PrizeStockConfiguredEvent event = new PrizeStockConfiguredEvent(
                prizeId, campaignId, oldQuantity, newQuantity, configVersion);
        streamBridge.send("prizeStockConfigured-out-0", event);
    }
}
