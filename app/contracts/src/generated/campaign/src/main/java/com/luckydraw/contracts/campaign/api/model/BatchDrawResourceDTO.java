package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.DrawResultResourceDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 批次抽獎結果（&#x60;count&#x3D;N≥2&#x60; 時回傳的 &#x60;data&#x60;）。承載 N 筆獨立抽選結果（UC-5）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class BatchDrawResourceDTO implements PostCampaignDrawResponseDTOData {

  @Valid
  private List<@Valid DrawResultResourceDTO> draws = new ArrayList<>();

  public BatchDrawResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchDrawResourceDTO(List<@Valid DrawResultResourceDTO> draws) {
    this.draws = draws;
  }

  public BatchDrawResourceDTO draws(List<@Valid DrawResultResourceDTO> draws) {
    this.draws = draws;
    return this;
  }

  public BatchDrawResourceDTO addDrawsItem(DrawResultResourceDTO drawsItem) {
    if (this.draws == null) {
      this.draws = new ArrayList<>();
    }
    this.draws.add(drawsItem);
    return this;
  }

  /**
   * N 筆抽獎結果（與 `count` 一致；每筆獨立計算機率與庫存）。
   * @return draws
   */
  @NotNull @Valid @Size(min = 2) 
  @JsonProperty("draws")
  public List<@Valid DrawResultResourceDTO> getDraws() {
    return draws;
  }

  public void setDraws(List<@Valid DrawResultResourceDTO> draws) {
    this.draws = draws;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchDrawResourceDTO batchDrawResourceDTO = (BatchDrawResourceDTO) o;
    return Objects.equals(this.draws, batchDrawResourceDTO.draws);
  }

  @Override
  public int hashCode() {
    return Objects.hash(draws);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchDrawResourceDTO {\n");
    sb.append("    draws: ").append(toIndentedString(draws)).append("\n");
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

