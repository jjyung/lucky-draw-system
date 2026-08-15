package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.PrizeInputDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 整批配置獎品請求體（UC-2）。**全量覆蓋**語意：client 送出完整獎品清單（含銘謝惠顧）， 獎品 &#x60;id&#x60; 由系統重建；已發生抽獎結果不受影響（僅後續抽獎採用新配置）。 驗證（總和 100%／越界／缺 THANK_YOU）失敗 → 整筆配置不生效（&#x60;422&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PutCampaignPrizesRequestDTO {

  @Valid
  private List<@Valid PrizeInputDTO> prizes = new ArrayList<>();

  public PutCampaignPrizesRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PutCampaignPrizesRequestDTO(List<@Valid PrizeInputDTO> prizes) {
    this.prizes = prizes;
  }

  public PutCampaignPrizesRequestDTO prizes(List<@Valid PrizeInputDTO> prizes) {
    this.prizes = prizes;
    return this;
  }

  public PutCampaignPrizesRequestDTO addPrizesItem(PrizeInputDTO prizesItem) {
    if (this.prizes == null) {
      this.prizes = new ArrayList<>();
    }
    this.prizes.add(prizesItem);
    return this;
  }

  /**
   * 完整獎品清單（含至少一個 `THANK_YOU`）。
   * @return prizes
   */
  @NotNull @Valid @Size(min = 1) 
  @JsonProperty("prizes")
  public List<@Valid PrizeInputDTO> getPrizes() {
    return prizes;
  }

  public void setPrizes(List<@Valid PrizeInputDTO> prizes) {
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
    PutCampaignPrizesRequestDTO putCampaignPrizesRequestDTO = (PutCampaignPrizesRequestDTO) o;
    return Objects.equals(this.prizes, putCampaignPrizesRequestDTO.prizes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prizes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PutCampaignPrizesRequestDTO {\n");
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

