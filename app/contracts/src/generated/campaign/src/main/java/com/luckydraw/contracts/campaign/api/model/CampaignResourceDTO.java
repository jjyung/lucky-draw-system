package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.CampaignStatusEnum;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 活動資源（管理端回傳，ADMIN）。含完整欄位（**含 &#x60;drawLimit&#x60;**）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class CampaignResourceDTO {

  private Long id;

  private String name;

  private CampaignStatusEnum status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime endTime;

  private Integer drawLimit;

  public CampaignResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CampaignResourceDTO(Long id, String name, CampaignStatusEnum status, OffsetDateTime startTime, OffsetDateTime endTime, Integer drawLimit) {
    this.id = id;
    this.name = name;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.drawLimit = drawLimit;
  }

  public CampaignResourceDTO id(Long id) {
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

  public CampaignResourceDTO name(String name) {
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

  public CampaignResourceDTO status(CampaignStatusEnum status) {
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

  public CampaignResourceDTO startTime(OffsetDateTime startTime) {
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

  public CampaignResourceDTO endTime(OffsetDateTime endTime) {
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

  public CampaignResourceDTO drawLimit(Integer drawLimit) {
    this.drawLimit = drawLimit;
    return this;
  }

  /**
   * 每使用者活動期間總抽獎次數上限（管理欄位）。
   * minimum: 1
   * @return drawLimit
   */
  @NotNull @Min(1) 
  @JsonProperty("drawLimit")
  public Integer getDrawLimit() {
    return drawLimit;
  }

  public void setDrawLimit(Integer drawLimit) {
    this.drawLimit = drawLimit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CampaignResourceDTO campaignResourceDTO = (CampaignResourceDTO) o;
    return Objects.equals(this.id, campaignResourceDTO.id) &&
        Objects.equals(this.name, campaignResourceDTO.name) &&
        Objects.equals(this.status, campaignResourceDTO.status) &&
        Objects.equals(this.startTime, campaignResourceDTO.startTime) &&
        Objects.equals(this.endTime, campaignResourceDTO.endTime) &&
        Objects.equals(this.drawLimit, campaignResourceDTO.drawLimit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, status, startTime, endTime, drawLimit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CampaignResourceDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    drawLimit: ").append(toIndentedString(drawLimit)).append("\n");
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

