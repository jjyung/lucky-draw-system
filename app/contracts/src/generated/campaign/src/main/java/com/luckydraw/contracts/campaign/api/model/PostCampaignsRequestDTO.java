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
 * 建立活動請求體（UC-1）。&#x60;id&#x60;/&#x60;status&#x60; 由伺服端產生（&#x60;status&#x60; 固定 &#x60;DRAFT&#x60;），client 不得指定。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostCampaignsRequestDTO {

  private String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime endTime;

  private Integer drawLimit;

  public PostCampaignsRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostCampaignsRequestDTO(String name, OffsetDateTime startTime, OffsetDateTime endTime, Integer drawLimit) {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.drawLimit = drawLimit;
  }

  public PostCampaignsRequestDTO name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 活動名稱（非空，對應 `campaigns.name`）。
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

  public PostCampaignsRequestDTO startTime(OffsetDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * 活動開始時間（可抽獎起點；需早於 `endTime`，對應 `campaigns.start_time`）。
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

  public PostCampaignsRequestDTO endTime(OffsetDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * 活動結束時間（可抽獎終點，需晚於 `startTime`）；個人次數計數 TTL 對齊此時間（SA UC-1）。 對應 `campaigns.end_time`。 
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

  public PostCampaignsRequestDTO drawLimit(Integer drawLimit) {
    this.drawLimit = drawLimit;
    return this;
  }

  /**
   * **每個使用者於本活動整個週期的總抽獎次數上限**（活動期間總額，非每日重置；對應 `campaigns.draw_limit`）。 正整數 ≥ 1（`chk_campaigns_draw_limit`）。 
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
    PostCampaignsRequestDTO postCampaignsRequestDTO = (PostCampaignsRequestDTO) o;
    return Objects.equals(this.name, postCampaignsRequestDTO.name) &&
        Objects.equals(this.startTime, postCampaignsRequestDTO.startTime) &&
        Objects.equals(this.endTime, postCampaignsRequestDTO.endTime) &&
        Objects.equals(this.drawLimit, postCampaignsRequestDTO.drawLimit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, startTime, endTime, drawLimit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostCampaignsRequestDTO {\n");
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

