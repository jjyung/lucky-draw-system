package com.luckydraw.contracts.inventory.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 庫存真相來源列的快照（&#x60;inventory&#x60; table，ADR-006）。**非事件 payload**， 僅作為事件消費後 DB 落點之輔助說明。&#x60;stock&#x60; 為唯一真相來源， 條件更新（&#x60;WHERE stock &gt; 0&#x60; / &#x60;WHERE stock + delta &gt;&#x3D; 0&#x60;）與 &#x60;CHECK (stock &gt;&#x3D; 0)&#x60; 保證絕不為負。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class InventoryState {

  private Long prizeId;

  private Integer stock;

  private Integer version;

  private Integer lastConfigVersion;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public InventoryState prizeId(Long prizeId) {
    this.prizeId = prizeId;
    return this;
  }

  /**
   * 對應獎品識別（`uq_inventory_prize_id` UNIQUE；THANK_YOU 無此列）。
   * @return prizeId
   */
  
  @JsonProperty("prizeId")
  public Long getPrizeId() {
    return prizeId;
  }

  public void setPrizeId(Long prizeId) {
    this.prizeId = prizeId;
  }

  public InventoryState stock(Integer stock) {
    this.stock = stock;
    return this;
  }

  /**
   * 剩餘可出貨數量（唯一真相）。扣減單向，但可因獎品 `quantity` 配置同步增減（ADR-010），不得為負。
   * minimum: 0
   * @return stock
   */
  @Min(0) 
  @JsonProperty("stock")
  public Integer getStock() {
    return stock;
  }

  public void setStock(Integer stock) {
    this.stock = stock;
  }

  public InventoryState version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * 樂觀鎖版本號（app 層條件更新輔助，ADR-003 替代方案保留）。
   * minimum: 0
   * @return version
   */
  @Min(0) 
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public InventoryState lastConfigVersion(Integer lastConfigVersion) {
    this.lastConfigVersion = lastConfigVersion;
    return this;
  }

  /**
   * 最近套用的 `prize-stock-configured` 版本；冪等去重與亂序排序鍵（ADR-010）。
   * minimum: 0
   * @return lastConfigVersion
   */
  @Min(0) 
  @JsonProperty("lastConfigVersion")
  public Integer getLastConfigVersion() {
    return lastConfigVersion;
  }

  public void setLastConfigVersion(Integer lastConfigVersion) {
    this.lastConfigVersion = lastConfigVersion;
  }

  public InventoryState updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * 最後異動時間（UTC）。
   * @return updatedAt
   */
  @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InventoryState inventoryState = (InventoryState) o;
    return Objects.equals(this.prizeId, inventoryState.prizeId) &&
        Objects.equals(this.stock, inventoryState.stock) &&
        Objects.equals(this.version, inventoryState.version) &&
        Objects.equals(this.lastConfigVersion, inventoryState.lastConfigVersion) &&
        Objects.equals(this.updatedAt, inventoryState.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prizeId, stock, version, lastConfigVersion, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InventoryState {\n");
    sb.append("    prizeId: ").append(toIndentedString(prizeId)).append("\n");
    sb.append("    stock: ").append(toIndentedString(stock)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    lastConfigVersion: ").append(toIndentedString(lastConfigVersion)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

