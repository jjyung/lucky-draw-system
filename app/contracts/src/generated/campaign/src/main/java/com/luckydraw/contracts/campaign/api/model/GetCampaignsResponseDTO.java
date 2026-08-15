package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.campaign.api.model.CampaignSummaryResourceDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 活動列表回應 envelope。&#x60;data&#x60; 承載 &#x60;CampaignSummaryResourceDTO&#x60; 陣列（不含 &#x60;drawLimit&#x60;）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class GetCampaignsResponseDTO {

  private String code;

  private String message;

  @Valid
  private List<@Valid CampaignSummaryResourceDTO> data = new ArrayList<>();

  public GetCampaignsResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetCampaignsResponseDTO(String code, String message, List<@Valid CampaignSummaryResourceDTO> data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public GetCampaignsResponseDTO code(String code) {
    this.code = code;
    return this;
  }

  /**
   * 成功碼，固定 `\"00000\"`。
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

  public GetCampaignsResponseDTO message(String message) {
    this.message = message;
    return this;
  }

  /**
   * 人類可讀訊息，成功通常 `\"OK\"`。
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

  public GetCampaignsResponseDTO data(List<@Valid CampaignSummaryResourceDTO> data) {
    this.data = data;
    return this;
  }

  public GetCampaignsResponseDTO addDataItem(CampaignSummaryResourceDTO dataItem) {
    if (this.data == null) {
      this.data = new ArrayList<>();
    }
    this.data.add(dataItem);
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public List<@Valid CampaignSummaryResourceDTO> getData() {
    return data;
  }

  public void setData(List<@Valid CampaignSummaryResourceDTO> data) {
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
    GetCampaignsResponseDTO getCampaignsResponseDTO = (GetCampaignsResponseDTO) o;
    return Objects.equals(this.code, getCampaignsResponseDTO.code) &&
        Objects.equals(this.message, getCampaignsResponseDTO.message) &&
        Objects.equals(this.data, getCampaignsResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetCampaignsResponseDTO {\n");
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

