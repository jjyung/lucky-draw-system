package com.luckydraw.campaign.event;

/**
 * prize-stock-configured 事件發布抽象（ADR-010）。
 * 抽象出來讓 PrizeService 可測；實作由 Spring Cloud Stream binder 發送。
 */
public interface PrizeStockEventPublisher {

    /**
     * 發布獎品庫存配置同步事件（獎品建立或 quantity 修改時；THANK_YOU 不發布）。
     *
     * @param prizeId       獎品識別（upsert 鍵）
     * @param campaignId    所屬活動
     * @param oldQuantity   修改前 quantity（新獎品為 0）
     * @param newQuantity   修改後 quantity
     * @param configVersion 每獎品單調遞增的配置版本
     */
    void publishPrizeStockConfigured(Long prizeId, Long campaignId,
                                     int oldQuantity, int newQuantity, int configVersion);
}
