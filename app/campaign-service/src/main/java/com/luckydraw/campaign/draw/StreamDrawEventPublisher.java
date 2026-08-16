package com.luckydraw.campaign.draw;

import com.luckydraw.campaign.model.entity.DrawRecordEntity;
import com.luckydraw.contracts.inventory.api.model.InventoryCommitEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * DrawEventPublisher 的 Spring Cloud Stream 實作（ADR-007）。
 * 發布 inventory-commit（drawRecordId 為冪等鍵，ADR-006），binding 見 application.yml。
 */
@Component
public class StreamDrawEventPublisher implements DrawEventPublisher {

    private final StreamBridge streamBridge;

    public StreamDrawEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publishInventoryCommit(DrawRecordEntity record) {
        InventoryCommitEvent event = new InventoryCommitEvent(
                record.getId(),
                record.getPrize().getId(),
                1);
        streamBridge.send("inventoryCommit-out-0", event);
    }
}
