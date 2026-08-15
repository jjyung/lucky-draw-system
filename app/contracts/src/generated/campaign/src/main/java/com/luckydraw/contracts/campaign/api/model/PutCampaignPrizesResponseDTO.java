package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.PrizesConfigResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 獎品配置回應 envelope。&#x60;data&#x60; 承載 &#x60;PrizesConfigResourceDTO&#x60;（含系統產生的 id）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PutCampaignPrizesResponseDTO {

  private String code;

  private String message;

  private PrizesConfigResourceDTO data;

  public PutCampaignPrizesResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PutCampaignPrizesResponseDTO(String code, String message, PrizesConfigResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public PutCampaignPrizesResponseDTO code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
   */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public PutCampaignPrizesResponseDTO message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull 
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public PutCampaignPrizesResponseDTO data(PrizesConfigResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public PrizesConfigResourceDTO getData() {
    return data;
  }

  public void setData(PrizesConfigResourceDTO data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PutCampaignPrizesResponseDTO putCampaignPrizesResponseDTO = (PutCampaignPrizesResponseDTO) o;
    return Objects.equals(this.code, putCampaignPrizesResponseDTO.code) &&
        Objects.equals(this.message, putCampaignPrizesResponseDTO.message) &&
        Objects.equals(this.data, putCampaignPrizesResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PutCampaignPrizesResponseDTO {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
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

