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
 * 獎品庫存配置同步事件（binding: &#x60;prize-stock-configured&#x60;，ADR-010）。 生產者：campaign-service；消費者：inventory-service。 campaign-service 於獎品建立或 &#x60;quantity&#x60; 修改時發布；&#x60;THANK_YOU&#x60; 獎品不發布 （銘謝惠顧不扣庫存，&#x60;stock&#x60; 忽略）。  **冪等／排序語意**： - 以 &#x60;prizeId&#x60; 做 **upsert 鍵**（&#x60;uq_inventory_prize_id&#x60; UNIQUE）。 - 以 &#x60;configVersion&#x60; 做 **冪等去重與排序**：僅   &#x60;incomingConfigVersion &gt; inventory.last_config_version&#x60; 時套用；   相同版本重投 → 跳過；較低版本 → 亂序／過期 → 跳過。  **消費語意**（&#x60;delta &#x3D; newQuantity - oldQuantity&#x60;，由 inventory 推導）： 1. 列不存在（首次建置）→ &#x60;INSERT&#x60;（&#x60;stock &#x3D; newQuantity&#x60;、&#x60;last_config_version &#x3D; configVersion&#x60;）。 2. 列已存在 → 條件更新：    &#x60;UPDATE inventory SET stock &#x3D; stock + delta, last_config_version &#x3D; ? WHERE prize_id &#x3D; ? AND stock + delta &gt;&#x3D; 0&#x60;。 3. rowcount &#x3D; 1 → 套用成功（增加 &#x3D; 補貨、減少 &#x3D; 縮減）。 4. rowcount &#x3D; 0 → 新總量 &lt; 已發放數 → **拒絕（不 clamp）**：&#x60;stock&#x60; 不變、記錄衝突 + 告警，    由對帳收斂。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PrizeStockConfiguredEvent {

  private Long prizeId;

  private Long campaignId;

  private Integer oldQuantity;

  private Integer newQuantity;

  private Integer configVersion;

  public PrizeStockConfiguredEvent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrizeStockConfiguredEvent(Long prizeId, Long campaignId, Integer oldQuantity, Integer newQuantity, Integer configVersion) {
    this.prizeId = prizeId;
    this.campaignId = campaignId;
    this.oldQuantity = oldQuantity;
    this.newQuantity = newQuantity;
    this.configVersion = configVersion;
  }

  public PrizeStockConfiguredEvent prizeId(Long prizeId) {
    this.prizeId = prizeId;
    return this;
  }

  /**
   * **upsert／冪等鍵**。對應 campaign.prizes.id（邏輯引用，無跨 DB FK）。 `uq_inventory_prize_id` UNIQUE 承接 upsert。 
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

  public PrizeStockConfiguredEvent campaignId(Long campaignId) {
    this.campaignId = campaignId;
    return this;
  }

  /**
   * 路由與稽核用（邏輯引用 campaign.campaigns.id）。
   * @return campaignId
   */
  @NotNull 
  @JsonProperty("campaignId")
  public Long getCampaignId() {
    return campaignId;
  }

  public void setCampaignId(Long campaignId) {
    this.campaignId = campaignId;
  }

  public PrizeStockConfiguredEvent oldQuantity(Integer oldQuantity) {
    this.oldQuantity = oldQuantity;
    return this;
  }

  /**
   * 修改前 quantity。campaign 於更新交易內**原子取得** old 值，是 config 真相。 
   * minimum: 0
   * @return oldQuantity
   */
  @NotNull @Min(0) 
  @JsonProperty("oldQuantity")
  public Integer getOldQuantity() {
    return oldQuantity;
  }

  public void setOldQuantity(Integer oldQuantity) {
    this.oldQuantity = oldQuantity;
  }

  public PrizeStockConfiguredEvent newQuantity(Integer newQuantity) {
    this.newQuantity = newQuantity;
    return this;
  }

  /**
   * 修改後 quantity。delta = `newQuantity - oldQuantity` 由 inventory 推導（帶絕對值使 event 可稽核、可重放）。 
   * minimum: 0
   * @return newQuantity
   */
  @NotNull @Min(0) 
  @JsonProperty("newQuantity")
  public Integer getNewQuantity() {
    return newQuantity;
  }

  public void setNewQuantity(Integer newQuantity) {
    this.newQuantity = newQuantity;
  }

  public PrizeStockConfiguredEvent configVersion(Integer configVersion) {
    this.configVersion = configVersion;
    return this;
  }

  /**
   * **每獎品單調遞增**的配置版本，用於冪等去重與排序（對應 `inventory.last_config_version`）。 僅 `incomingVersion > last_config_version` 時套用。 
   * minimum: 0
   * @return configVersion
   */
  @NotNull @Min(0) 
  @JsonProperty("configVersion")
  public Integer getConfigVersion() {
    return configVersion;
  }

  public void setConfigVersion(Integer configVersion) {
    this.configVersion = configVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrizeStockConfiguredEvent prizeStockConfiguredEvent = (PrizeStockConfiguredEvent) o;
    return Objects.equals(this.prizeId, prizeStockConfiguredEvent.prizeId) &&
        Objects.equals(this.campaignId, prizeStockConfiguredEvent.campaignId) &&
        Objects.equals(this.oldQuantity, prizeStockConfiguredEvent.oldQuantity) &&
        Objects.equals(this.newQuantity, prizeStockConfiguredEvent.newQuantity) &&
        Objects.equals(this.configVersion, prizeStockConfiguredEvent.configVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prizeId, campaignId, oldQuantity, newQuantity, configVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrizeStockConfiguredEvent {\n");
    sb.append("    prizeId: ").append(toIndentedString(prizeId)).append("\n");
    sb.append("    campaignId: ").append(toIndentedString(campaignId)).append("\n");
    sb.append("    oldQuantity: ").append(toIndentedString(oldQuantity)).append("\n");
    sb.append("    newQuantity: ").append(toIndentedString(newQuantity)).append("\n");
    sb.append("    configVersion: ").append(toIndentedString(configVersion)).append("\n");
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

