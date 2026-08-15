package com.luckydraw.contracts.inventory.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 庫存扣減事件（binding: &#x60;inventory-commit&#x60;，ADR-006/007）。 生產者：campaign-service；消費者：inventory-service。  **delivery semantics：at-least-once**（可能重複投遞）。 consumer 以 &#x60;drawRecordId&#x60; 冪等去重；同一 &#x60;drawRecordId&#x60; 至多觸發一次出貨真相扣減。  **消費語意**： 1. 冪等檢查 &#x60;reservations.draw_record_id&#x60;；已存在 → 直接 ack，不重複扣減。 2. 條件更新（真相扣減）：&#x60;UPDATE inventory SET stock &#x3D; stock - quantity WHERE prize_id &#x3D; ? AND stock &gt; 0&#x60;。 3. rowcount &#x3D; 1 → &#x60;reservations.status &#x3D; COMMITTED&#x60;（扣減成功）。 4. rowcount &#x3D; 0 → 庫存不足 → 補償（UC-2）：&#x60;reservations.status &#x3D; REVERSED&#x60;    + 校正即時判定層 + 告警。**絕不負庫存**（&#x60;FR-INV-02&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class InventoryCommitEvent {

  private Long drawRecordId;

  private Long prizeId;

  private Integer quantity = 1;

  public InventoryCommitEvent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InventoryCommitEvent(Long drawRecordId, Long prizeId, Integer quantity) {
    this.drawRecordId = drawRecordId;
    this.prizeId = prizeId;
    this.quantity = quantity;
  }

  public InventoryCommitEvent drawRecordId(Long drawRecordId) {
    this.drawRecordId = drawRecordId;
    return this;
  }

  /**
   * **冪等鍵（唯一）**。對應 campaign.draw_records.id（邏輯引用，無跨 DB FK，ADR-002）。 由上游抽獎記錄決定、事件攜帶；consumer **不得自行生成**。 重複投遞撞此鍵即忽略（`reservations.draw_record_id` UNIQUE，ADR-005/006）。 
   * @return drawRecordId
   */
  @NotNull 
  @JsonProperty("drawRecordId")
  public Long getDrawRecordId() {
    return drawRecordId;
  }

  public void setDrawRecordId(Long drawRecordId) {
    this.drawRecordId = drawRecordId;
  }

  public InventoryCommitEvent prizeId(Long prizeId) {
    this.prizeId = prizeId;
    return this;
  }

  /**
   * 扣減之獎品識別（邏輯引用 campaign.prizes.id，無跨 DB FK）。
   * @return prizeId
   */
  @NotNull 
  @JsonProperty("prizeId")
  public Long getPrizeId() {
    return prizeId;
  }

  public void setPrizeId(Long prizeId) {
    this.prizeId = prizeId;
  }

  public InventoryCommitEvent quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * 扣減件數。**POC 階段恆 = 1**（每次抽獎一件，`chk_reservations_quantity` 亦限 `>= 1`）。 
   * minimum: 1
   * @return quantity
   */
  @NotNull @Min(1) 
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InventoryCommitEvent inventoryCommitEvent = (InventoryCommitEvent) o;
    return Objects.equals(this.drawRecordId, inventoryCommitEvent.drawRecordId) &&
        Objects.equals(this.prizeId, inventoryCommitEvent.prizeId) &&
        Objects.equals(this.quantity, inventoryCommitEvent.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(drawRecordId, prizeId, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InventoryCommitEvent {\n");
    sb.append("    drawRecordId: ").append(toIndentedString(drawRecordId)).append("\n");
    sb.append("    prizeId: ").append(toIndentedString(prizeId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

