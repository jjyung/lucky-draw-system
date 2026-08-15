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
 * 獎品資源（管理端回傳，ADMIN）。含完整欄位（含 &#x60;probability&#x60;/&#x60;quantity&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PrizeResourceDTO {

  private Long id;

  private String name;

  private PrizeType type;

  private BigDecimal probability;

  private Integer quantity;

  public PrizeResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrizeResourceDTO(Long id, String name, PrizeType type, BigDecimal probability, Integer quantity) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.probability = probability;
    this.quantity = quantity;
  }

  public PrizeResourceDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * 獎品唯一識別（系統產生）。
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

  public PrizeResourceDTO name(String name) {
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

  public PrizeResourceDTO type(PrizeType type) {
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

  public PrizeResourceDTO probability(BigDecimal probability) {
    this.probability = probability;
    return this;
  }

  /**
   * 中獎機率（百分比）；全體總和 = 100%。
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

  public PrizeResourceDTO quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * 可發放數量；`THANK_YOU` 為 0 且忽略（無限庫存）。
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
    PrizeResourceDTO prizeResourceDTO = (PrizeResourceDTO) o;
    return Objects.equals(this.id, prizeResourceDTO.id) &&
        Objects.equals(this.name, prizeResourceDTO.name) &&
        Objects.equals(this.type, prizeResourceDTO.type) &&
        Objects.equals(this.probability, prizeResourceDTO.probability) &&
        Objects.equals(this.quantity, prizeResourceDTO.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, type, probability, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrizeResourceDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

