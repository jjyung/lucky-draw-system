package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.PrizeResourceDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 獎品配置結果（&#x60;putCampaignPrizes&#x60; 回傳的 &#x60;data&#x60;）。為已生效的完整獎品清單（含系統產生的 &#x60;id&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PrizesConfigResourceDTO {

  private Long campaignId;

  @Valid
  private List<@Valid PrizeResourceDTO> prizes = new ArrayList<>();

  public PrizesConfigResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrizesConfigResourceDTO(Long campaignId, List<@Valid PrizeResourceDTO> prizes) {
    this.campaignId = campaignId;
    this.prizes = prizes;
  }

  public PrizesConfigResourceDTO campaignId(Long campaignId) {
    this.campaignId = campaignId;
    return this;
  }

  /**
   * 所屬活動唯一識別。
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

  public PrizesConfigResourceDTO prizes(List<@Valid PrizeResourceDTO> prizes) {
    this.prizes = prizes;
    return this;
  }

  public PrizesConfigResourceDTO addPrizesItem(PrizeResourceDTO prizesItem) {
    if (this.prizes == null) {
      this.prizes = new ArrayList<>();
    }
    this.prizes.add(prizesItem);
    return this;
  }

  /**
   * 生效的獎品清單（含 THANK_YOU）。
   * @return prizes
   */
  @NotNull @Valid 
  @JsonProperty("prizes")
  public List<@Valid PrizeResourceDTO> getPrizes() {
    return prizes;
  }

  public void setPrizes(List<@Valid PrizeResourceDTO> prizes) {
    this.prizes = prizes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrizesConfigResourceDTO prizesConfigResourceDTO = (PrizesConfigResourceDTO) o;
    return Objects.equals(this.campaignId, prizesConfigResourceDTO.campaignId) &&
        Objects.equals(this.prizes, prizesConfigResourceDTO.prizes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(campaignId, prizes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrizesConfigResourceDTO {\n");
    sb.append("    campaignId: ").append(toIndentedString(campaignId)).append("\n");
    sb.append("    prizes: ").append(toIndentedString(prizes)).append("\n");
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

