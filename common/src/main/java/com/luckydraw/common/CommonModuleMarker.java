package com.luckydraw.common;

/**
 * common module 的佔位類別。
 * 依 ADR-001/007，跨服務共用的 DTO、Event payload（InventoryCommitEvent、
 * PrizeStockConfiguredEvent 等）與共用例外將置於此 module。
 * Slice 0 僅建立骨架，實際內容於對應 slice 補入。
 */
public final class CommonModuleMarker {
    private CommonModuleMarker() {
    }
}
