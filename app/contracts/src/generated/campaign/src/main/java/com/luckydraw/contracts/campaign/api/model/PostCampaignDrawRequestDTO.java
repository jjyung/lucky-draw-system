package com.luckydraw.contracts.campaign.api.model;

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
 * 抽獎請求體（UC-4 單次 / UC-5 批次）。&#x60;count&#x60; 可省略（預設 1）。&#x60;count&#x3D;1&#x60; 單次； &#x60;count&#x3D;N≥2&#x60; 批次（整批由**單一** Idempotency-Key 保護，一次扣 N 次）。 中獎結果由伺服端演算法決定，client 不得指定。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostCampaignDrawRequestDTO {

  private Integer count = 1;

  public PostCampaignDrawRequestDTO count(Integer count) {
    this.count = count;
    return this;
  }

  /**
   * 抽獎次數（預設 1）。`count ≥ 1`；`count=N≥2` 為批次。若剩餘次數 < N → 整批不執行 → `429`（`A0306`）。 上限由 `drawLimit`（活動期間總額）於 runtime 判定，非 schema 硬上限。 
   * minimum: 1
   * @return count
   */
  @Min(1) 
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostCampaignDrawRequestDTO postCampaignDrawRequestDTO = (PostCampaignDrawRequestDTO) o;
    return Objects.equals(this.count, postCampaignDrawRequestDTO.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostCampaignDrawRequestDTO {\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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

