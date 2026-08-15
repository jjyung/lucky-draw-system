package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 刷新成功回傳的新存取憑證（UC-3 / AC-AUTH-013）。身份與角色**沿用原憑證**、僅更新有效期。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class AccessTokenResourceDTO {

  private String accessToken;

  private String tokenType;

  private Integer expiresIn;

  public AccessTokenResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AccessTokenResourceDTO(String accessToken, String tokenType, Integer expiresIn) {
    this.accessToken = accessToken;
    this.tokenType = tokenType;
    this.expiresIn = expiresIn;
  }

  public AccessTokenResourceDTO accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * 新的存取憑證（JWT RS256），身份/角色不變。
   * @return accessToken
   */
  @NotNull 
  @JsonProperty("accessToken")
  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public AccessTokenResourceDTO tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  /**
   * 憑證類型，固定 `Bearer`。
   * @return tokenType
   */
  @NotNull 
  @JsonProperty("tokenType")
  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public AccessTokenResourceDTO expiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
    return this;
  }

  /**
   * 新存取憑證有效秒數。
   * minimum: 0
   * @return expiresIn
   */
  @NotNull @Min(0) 
  @JsonProperty("expiresIn")
  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessTokenResourceDTO accessTokenResourceDTO = (AccessTokenResourceDTO) o;
    return Objects.equals(this.accessToken, accessTokenResourceDTO.accessToken) &&
        Objects.equals(this.tokenType, accessTokenResourceDTO.tokenType) &&
        Objects.equals(this.expiresIn, accessTokenResourceDTO.expiresIn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, tokenType, expiresIn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccessTokenResourceDTO {\n");
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    expiresIn: ").append(toIndentedString(expiresIn)).append("\n");
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

