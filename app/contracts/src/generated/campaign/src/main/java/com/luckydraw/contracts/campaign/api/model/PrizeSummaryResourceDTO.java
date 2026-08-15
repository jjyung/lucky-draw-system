package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.PrizeTypeEnum;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 獎品摘要（PUBLIC / 抽獎結果承載）。僅含 &#x60;id/name/type&#x60;，**不暴露 &#x60;probability&#x60;/&#x60;quantity&#x60;**（敏感營運參數）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PrizeSummaryResourceDTO {

  private Long id;

  private String name;

  private PrizeTypeEnum type;

  public PrizeSummaryResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrizeSummaryResourceDTO(Long id, String name, PrizeTypeEnum type) {
    this.id = id;
    this.name = name;
    this.type = type;
  }

  public PrizeSummaryResourceDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * 獎品唯一識別（對應 `prizes.id`）。
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

  public PrizeSummaryResourceDTO name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 獎品名稱。
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

  public PrizeSummaryResourceDTO type(PrizeTypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public PrizeTypeEnum getType() {
    return type;
  }

  public void setType(PrizeTypeEnum type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrizeSummaryResourceDTO prizeSummaryResourceDTO = (PrizeSummaryResourceDTO) o;
    return Objects.equals(this.id, prizeSummaryResourceDTO.id) &&
        Objects.equals(this.name, prizeSummaryResourceDTO.name) &&
        Objects.equals(this.type, prizeSummaryResourceDTO.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrizeSummaryResourceDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

