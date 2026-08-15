package com.luckydraw.campaign.draw;

import com.luckydraw.campaign.model.entity.DrawRecordEntity;
import org.springframework.stereotype.Component;

/**
 * inventory-commit 事件發布的暫行實作。
 * Slice 2b 先落庫與回傳正確結果；實際 MQ 發布（Spring Cloud Stream binder）於
 * inventory-service slice 一起接線（ADR-007）。屆時以 StreamBridge 替換本類。
 */
@Component
public class NoopDrawEventPublisher implements DrawEventPublisher {

    @Override
    public void publishInventoryCommit(DrawRecordEntity record) {
        // 暫不發布；record.getId() 即 inventory-commit 的 drawRecordId 冪等鍵（ADR-006）
    }
}
