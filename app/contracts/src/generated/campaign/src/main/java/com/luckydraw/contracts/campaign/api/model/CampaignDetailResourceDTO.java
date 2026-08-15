package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.CampaignStatusEnum;
import com.luckydraw.contracts.campaign.api.model.PrizeSummaryResourceDTO;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 活動詳情（PUBLIC）。**不含 &#x60;drawLimit&#x60;**（管理欄位）；獎品清單僅回 &#x60;id/name/type&#x60;， **不暴露 &#x60;probability&#x60;/&#x60;quantity&#x60;**（SA §5.2 敏感營運參數）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class CampaignDetailResourceDTO {

  private Long id;

  private String name;

  private CampaignStatusEnum status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime endTime;

  @Valid
  private List<@Valid PrizeSummaryResourceDTO> prizes = new ArrayList<>();

  public CampaignDetailResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CampaignDetailResourceDTO(Long id, String name, CampaignStatusEnum status, OffsetDateTime startTime, OffsetDateTime endTime) {
    this.id = id;
    this.name = name;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public CampaignDetailResourceDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * 活動唯一識別。
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CampaignDetailResourceDTO name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 活動名稱。
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CampaignDetailResourceDTO status(CampaignStatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public CampaignStatusEnum getStatus() {
    return status;
  }

  public void setStatus(CampaignStatusEnum status) {
    this.status = status;
  }

  public CampaignDetailResourceDTO startTime(OffsetDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * 活動開始時間。
   * @return startTime
   */
  @NotNull @Valid 
  @JsonProperty("startTime")
  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime = startTime;
  }

  public CampaignDetailResourceDTO endTime(OffsetDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * 活動結束時間。
   * @return endTime
   */
  @NotNull @Valid 
  @JsonProperty("endTime")
  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime = endTime;
  }

  public CampaignDetailResourceDTO prizes(List<@Valid PrizeSummaryResourceDTO> prizes) {
    this.prizes = prizes;
    return this;
  }

  public CampaignDetailResourceDTO addPrizesItem(PrizeSummaryResourceDTO prizesItem) {
    if (this.prizes == null) {
      this.prizes = new ArrayList<>();
    }
    this.prizes.add(prizesItem);
    return this;
  }

  /**
   * 獎品清單（供展示「可贏得什麼」），僅含 `id/name/type`。
   * @return prizes
   */
  @Valid 
  @JsonProperty("prizes")
  public List<@Valid PrizeSummaryResourceDTO> getPrizes() {
    return prizes;
  }

  public void setPrizes(List<@Valid PrizeSummaryResourceDTO> prizes) {
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
    CampaignDetailResourceDTO campaignDetailResourceDTO = (CampaignDetailResourceDTO) o;
    return Objects.equals(this.id, campaignDetailResourceDTO.id) &&
        Objects.equals(this.name, campaignDetailResourceDTO.name) &&
        Objects.equals(this.status, campaignDetailResourceDTO.status) &&
        Objects.equals(this.startTime, campaignDetailResourceDTO.startTime) &&
        Objects.equals(this.endTime, campaignDetailResourceDTO.endTime) &&
        Objects.equals(this.prizes, campaignDetailResourceDTO.prizes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, status, startTime, endTime, prizes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CampaignDetailResourceDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
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

