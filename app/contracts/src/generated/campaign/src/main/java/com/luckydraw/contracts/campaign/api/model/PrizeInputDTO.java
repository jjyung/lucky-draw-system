package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.PrizeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 獎品配置輸入項（UC-2）。&#x60;probability&#x60; ∈ &#x60;[0,100]&#x60;；全體（含 THANK_YOU）總和 &#x3D; 100%。 &#x60;quantity&#x60; 僅 &#x60;PRIZE&#x60; 有意義（可發放數量，非負整數）；&#x60;THANK_YOU&#x60; 忽略（無限庫存，可填 0）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PrizeInputDTO {

  private String name;

  private PrizeType type;

  private BigDecimal probability;

  private Integer quantity;

  public PrizeInputDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrizeInputDTO(String name, PrizeType type, BigDecimal probability, Integer quantity) {
    this.name = name;
    this.type = type;
    this.probability = probability;
    this.quantity = quantity;
  }

  public PrizeInputDTO name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 獎品名稱（非空，對應 `prizes.name`）。
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

  public PrizeInputDTO type(PrizeType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public PrizeType getType() {
    return type;
  }

  public void setType(PrizeType type) {
    this.type = type;
  }

  public PrizeInputDTO probability(BigDecimal probability) {
    this.probability = probability;
    return this;
  }

  /**
   * 中獎機率（百分比）。含銘謝惠顧在內，全體總和必須 = 100%（浮點容差內，app-level 驗證， `FR-CAMP-04`）。越界 → `422`/`A0304`。對應 `prizes.probability`（NUMERIC(5,2)）。 
   * minimum: 0
   * maximum: 100
   * @return probability
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("100") 
  @JsonProperty("probability")
  public BigDecimal getProbability() {
    return probability;
  }

  public void setProbability(BigDecimal probability) {
    this.probability = probability;
  }

  public PrizeInputDTO quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * 可發放數量（對應 `prizes.stock`）。僅 `PRIZE` 有意義（非負整數）； `THANK_YOU` 忽略（無限庫存，值為 0）。為庫存 config 真相，同步至 inventory-service（ADR-010）。 
   * minimum: 0
   * @return quantity
   */
  @NotNull @Min(0) 
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrizeInputDTO prizeInputDTO = (PrizeInputDTO) o;
    return Objects.equals(this.name, prizeInputDTO.name) &&
        Objects.equals(this.type, prizeInputDTO.type) &&
        Objects.equals(this.probability, prizeInputDTO.probability) &&
        Objects.equals(this.quantity, prizeInputDTO.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, probability, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrizeInputDTO {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    probability: ").append(toIndentedString(probability)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
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

