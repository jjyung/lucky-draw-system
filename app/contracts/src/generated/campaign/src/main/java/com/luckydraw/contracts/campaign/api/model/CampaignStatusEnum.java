package com.luckydraw.contracts.campaign.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 活動生命週期狀態（SA §4.1）。狀態機：`DRAFT → ACTIVE → ENDED`（單向，`ENDED` 為終態不可回轉）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public enum CampaignStatusEnum {
  
  DRAFT("DRAFT"),
  
  ACTIVE("ACTIVE"),
  
  ENDED("ENDED");

  private String value;

  CampaignStatusEnum(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static CampaignStatusEnum fromValue(String value) {
    for (CampaignStatusEnum b : CampaignStatusEnum.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

