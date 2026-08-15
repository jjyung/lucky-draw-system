package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.DrawResultTypeEnum;
import com.luckydraw.contracts.campaign.api.model.PrizeSummaryResourceDTO;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 單筆抽獎結果（UC-4）。&#x60;resultType&#x3D;WIN&#x60; 時 &#x60;prize&#x60; 為中獎獎品； &#x60;resultType&#x3D;THANK_YOU&#x60; 時 &#x60;prize&#x3D;null&#x60;（對齊 &#x60;campaign-db.md&#x60; §3.3：THANK_YOU 落庫不帶 &#x60;prize_id&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class DrawResultResourceDTO implements PostCampaignDrawResponseDTOData {

  private Long drawRecordId;

  private Long campaignId;

  private DrawResultTypeEnum resultType;

  private PrizeSummaryResourceDTO prize = null;

  public DrawResultResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DrawResultResourceDTO(Long drawRecordId, Long campaignId, DrawResultTypeEnum resultType) {
    this.drawRecordId = drawRecordId;
    this.campaignId = campaignId;
    this.resultType = resultType;
  }

  public DrawResultResourceDTO drawRecordId(Long drawRecordId) {
    this.drawRecordId = drawRecordId;
    return this;
  }

  /**
   * 抽獎記錄唯一識別（對應 `draw_records.id`；亦為 `inventory-commit` 事件的冪等鍵，ADR-006）。
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

  public DrawResultResourceDTO campaignId(Long campaignId) {
    this.campaignId = campaignId;
    return this;
  }

  /**
   * 抽獎所屬活動識別（由請求路徑決定，非 client 指定）。
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

  public DrawResultResourceDTO resultType(DrawResultTypeEnum resultType) {
    this.resultType = resultType;
    return this;
  }

  /**
   * Get resultType
   * @return resultType
   */
  @NotNull @Valid 
  @JsonProperty("resultType")
  public DrawResultTypeEnum getResultType() {
    return resultType;
  }

  public void setResultType(DrawResultTypeEnum resultType) {
    this.resultType = resultType;
  }

  public DrawResultResourceDTO prize(PrizeSummaryResourceDTO prize) {
    this.prize = prize;
    return this;
  }

  /**
   * 中獎獎品（`WIN` 時存在；`THANK_YOU` 時為 `null`）。
   * @return prize
   */
  @Valid 
  @JsonProperty("prize")
  public PrizeSummaryResourceDTO getPrize() {
    return prize;
  }

  public void setPrize(PrizeSummaryResourceDTO prize) {
    this.prize = prize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DrawResultResourceDTO drawResultResourceDTO = (DrawResultResourceDTO) o;
    return Objects.equals(this.drawRecordId, drawResultResourceDTO.drawRecordId) &&
        Objects.equals(this.campaignId, drawResultResourceDTO.campaignId) &&
        Objects.equals(this.resultType, drawResultResourceDTO.resultType) &&
        Objects.equals(this.prize, drawResultResourceDTO.prize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(drawRecordId, campaignId, resultType, prize);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DrawResultResourceDTO {\n");
    sb.append("    drawRecordId: ").append(toIndentedString(drawRecordId)).append("\n");
    sb.append("    campaignId: ").append(toIndentedString(campaignId)).append("\n");
    sb.append("    resultType: ").append(toIndentedString(resultType)).append("\n");
    sb.append("    prize: ").append(toIndentedString(prize)).append("\n");
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

