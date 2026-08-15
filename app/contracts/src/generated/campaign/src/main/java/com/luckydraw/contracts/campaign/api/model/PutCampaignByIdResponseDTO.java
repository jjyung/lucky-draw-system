package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.CampaignResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 更新活動回應 envelope。&#x60;data&#x60; 承載更新後的 &#x60;CampaignResourceDTO&#x60;。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PutCampaignByIdResponseDTO {

  private String code;

  private String message;

  private CampaignResourceDTO data;

  public PutCampaignByIdResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PutCampaignByIdResponseDTO(String code, String message, CampaignResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public PutCampaignByIdResponseDTO code(String code) {
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

  public PutCampaignByIdResponseDTO message(String message) {
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

  public PutCampaignByIdResponseDTO data(CampaignResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public CampaignResourceDTO getData() {
    return data;
  }

  public void setData(CampaignResourceDTO data) {
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
    PutCampaignByIdResponseDTO putCampaignByIdResponseDTO = (PutCampaignByIdResponseDTO) o;
    return Objects.equals(this.code, putCampaignByIdResponseDTO.code) &&
        Objects.equals(this.message, putCampaignByIdResponseDTO.message) &&
        Objects.equals(this.data, putCampaignByIdResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PutCampaignByIdResponseDTO {\n");
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

