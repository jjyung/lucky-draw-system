package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.PostCampaignDrawResponseDTOData;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 抽獎回應 envelope（首次與 replay 逐位元一致，ADR-005）。 &#x60;data&#x60;：&#x60;count&#x3D;1&#x60; 回單一 &#x60;DrawResultResourceDTO&#x60;；&#x60;count≥2&#x60; 回 &#x60;BatchDrawResourceDTO&#x60;（&#x60;{ draws: [...] }&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostCampaignDrawResponseDTO {

  private String code;

  private String message;

  private PostCampaignDrawResponseDTOData data;

  public PostCampaignDrawResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostCampaignDrawResponseDTO(String code, String message, PostCampaignDrawResponseDTOData data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public PostCampaignDrawResponseDTO code(String code) {
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

  public PostCampaignDrawResponseDTO message(String message) {
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

  public PostCampaignDrawResponseDTO data(PostCampaignDrawResponseDTOData data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public PostCampaignDrawResponseDTOData getData() {
    return data;
  }

  public void setData(PostCampaignDrawResponseDTOData data) {
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
    PostCampaignDrawResponseDTO postCampaignDrawResponseDTO = (PostCampaignDrawResponseDTO) o;
    return Objects.equals(this.code, postCampaignDrawResponseDTO.code) &&
        Objects.equals(this.message, postCampaignDrawResponseDTO.message) &&
        Objects.equals(this.data, postCampaignDrawResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostCampaignDrawResponseDTO {\n");
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

