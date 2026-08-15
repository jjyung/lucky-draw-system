package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.luckydraw.contracts.campaign.api.model.CampaignStatus;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 狀態轉移請求體（UC-3）。合法轉移 &#x60;DRAFT → ACTIVE → ENDED&#x60;（單向，&#x60;ENDED&#x60; 終態不可回轉）； 非法轉移 → &#x60;409&#x60;（&#x60;A0302&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PatchCampaignStatusRequestDTO {

  private CampaignStatus status;

  public PatchCampaignStatusRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PatchCampaignStatusRequestDTO(CampaignStatus status) {
    this.status = status;
  }

  public PatchCampaignStatusRequestDTO status(CampaignStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public CampaignStatus getStatus() {
    return status;
  }

  public void setStatus(CampaignStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PatchCampaignStatusRequestDTO patchCampaignStatusRequestDTO = (PatchCampaignStatusRequestDTO) o;
    return Objects.equals(this.status, patchCampaignStatusRequestDTO.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PatchCampaignStatusRequestDTO {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

