package com.luckydraw.contracts.campaign.api.model;

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
 * 全量更新活動請求體（UC-1）。覆蓋 &#x60;name/startTime/endTime/drawLimit&#x60;，**不含 &#x60;status&#x60;**（狀態轉移走 PATCH status）。 僅可於可編輯狀態（&#x60;DRAFT&#x60;/&#x60;ACTIVE&#x60;）修改；&#x60;ENDED&#x60; 不可編輯 → &#x60;409&#x60;（&#x60;A0302&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PutCampaignByIdRequestDTO {

  private String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime endTime;

  private Integer drawLimit;

  public PutCampaignByIdRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PutCampaignByIdRequestDTO(String name, OffsetDateTime startTime, OffsetDateTime endTime, Integer drawLimit) {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.drawLimit = drawLimit;
  }

  public PutCampaignByIdRequestDTO name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 活動名稱（非空）。
   * @return name
   */
  @NotNull @Size(min = 1, max = 128) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PutCampaignByIdRequestDTO startTime(OffsetDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * 活動開始時間（需早於 `endTime`）。
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

  public PutCampaignByIdRequestDTO endTime(OffsetDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * 活動結束時間（需晚於 `startTime`）。
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

  public PutCampaignByIdRequestDTO drawLimit(Integer drawLimit) {
    this.drawLimit = drawLimit;
    return this;
  }

  /**
   * 每使用者活動期間總抽獎次數上限（正整數 ≥ 1）。
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
    PutCampaignByIdRequestDTO putCampaignByIdRequestDTO = (PutCampaignByIdRequestDTO) o;
    return Objects.equals(this.name, putCampaignByIdRequestDTO.name) &&
        Objects.equals(this.startTime, putCampaignByIdRequestDTO.startTime) &&
        Objects.equals(this.endTime, putCampaignByIdRequestDTO.endTime) &&
        Objects.equals(this.drawLimit, putCampaignByIdRequestDTO.drawLimit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, startTime, endTime, drawLimit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PutCampaignByIdRequestDTO {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

