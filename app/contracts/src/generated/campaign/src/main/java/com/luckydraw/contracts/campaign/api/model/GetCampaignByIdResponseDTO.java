package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.CampaignDetailResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 活動詳情回應 envelope。&#x60;data&#x60; 承載 &#x60;CampaignDetailResourceDTO&#x60;（含獎品清單，不含敏感欄位）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class GetCampaignByIdResponseDTO {

  private String code;

  private String message;

  private CampaignDetailResourceDTO data;

  public GetCampaignByIdResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetCampaignByIdResponseDTO(String code, String message, CampaignDetailResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public GetCampaignByIdResponseDTO code(String code) {
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

  public GetCampaignByIdResponseDTO message(String message) {
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

  public GetCampaignByIdResponseDTO data(CampaignDetailResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public CampaignDetailResourceDTO getData() {
    return data;
  }

  public void setData(CampaignDetailResourceDTO data) {
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
    GetCampaignByIdResponseDTO getCampaignByIdResponseDTO = (GetCampaignByIdResponseDTO) o;
    return Objects.equals(this.code, getCampaignByIdResponseDTO.code) &&
        Objects.equals(this.message, getCampaignByIdResponseDTO.message) &&
        Objects.equals(this.data, getCampaignByIdResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetCampaignByIdResponseDTO {\n");
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

