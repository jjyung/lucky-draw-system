package com.luckydraw.campaign.draw;

import com.luckydraw.campaign.model.entity.DrawRecordEntity;

/**
 * inventory-commit 事件發布抽象（ADR-007）。
 * 抽象出來讓 DrawService 可測；實作由 Spring Cloud Stream binder 發送。
 */
public interface DrawEventPublisher {

    /**
     * 發布一筆中獎的庫存扣減通知（僅 WIN 結果）。
     */
    void publishInventoryCommit(DrawRecordEntity record);
}
