package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.CampaignStatus;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 活動摘要（列表項，PUBLIC）。**不含 &#x60;drawLimit&#x60;**（管理欄位，避免洩漏控管參數）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class CampaignSummaryResourceDTO {

  private Long id;

  private String name;

  private CampaignStatus status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime endTime;

  public CampaignSummaryResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CampaignSummaryResourceDTO(Long id, String name, CampaignStatus status, OffsetDateTime startTime, OffsetDateTime endTime) {
    this.id = id;
    this.name = name;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public CampaignSummaryResourceDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * 活動唯一識別（對應 `campaigns.id`）。
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

  public CampaignSummaryResourceDTO name(String name) {
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

  public CampaignSummaryResourceDTO status(CampaignStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public CampaignStatus getStatus() {
    return status;
  }

  public void setStatus(CampaignStatus status) {
    this.status = status;
  }

  public CampaignSummaryResourceDTO startTime(OffsetDateTime startTime) {
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

  public CampaignSummaryResourceDTO endTime(OffsetDateTime endTime) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CampaignSummaryResourceDTO campaignSummaryResourceDTO = (CampaignSummaryResourceDTO) o;
    return Objects.equals(this.id, campaignSummaryResourceDTO.id) &&
        Objects.equals(this.name, campaignSummaryResourceDTO.name) &&
        Objects.equals(this.status, campaignSummaryResourceDTO.status) &&
        Objects.equals(this.startTime, campaignSummaryResourceDTO.startTime) &&
        Objects.equals(this.endTime, campaignSummaryResourceDTO.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, status, startTime, endTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CampaignSummaryResourceDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
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

