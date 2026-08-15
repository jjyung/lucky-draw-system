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
 * 登入成功簽發的憑證集合（UC-2 / AC-AUTH-004）。&#x60;accessToken&#x60; 為 RS256 JWT， claims 承載 &#x60;sub&#x60;（userId）、&#x60;roles&#x60;、&#x60;exp&#x60;、&#x60;iat&#x60;、&#x60;iss&#x60;（ADR-009）。 &#x60;refreshToken&#x60; 標記 **Should**（&#x60;FR-AUTH-04&#x60;），POC 可先不實作。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class TokenResourceDTO {

  private String accessToken;

  private String tokenType;

  private Integer expiresIn;

  private String refreshToken;

  public TokenResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TokenResourceDTO(String accessToken, String tokenType, Integer expiresIn) {
    this.accessToken = accessToken;
    this.tokenType = tokenType;
    this.expiresIn = expiresIn;
  }

  public TokenResourceDTO accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * 存取憑證（JWT RS256）。承載身份/角色/有效期/簽發時刻/簽發者。
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

  public TokenResourceDTO tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  /**
   * 憑證類型，固定 `Bearer`（對應 Authorization header 的 Bearer scheme）。
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

  public TokenResourceDTO expiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
    return this;
  }

  /**
   * 存取憑證有效秒數（對應 JWT `exp - iat`）。POC 採短 TTL（如 1800，ADR-009 §Consequences）。
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

  public TokenResourceDTO refreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

  /**
   * 刷新憑證（JWT，**Should**，`FR-AUTH-04`）。用於 `POST /auth/refresh` 延長登入有效期， 不改變身份或權限。POC 未實作時此欄位可省略。 
   * @return refreshToken
   */
  
  @JsonProperty("refreshToken")
  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TokenResourceDTO tokenResourceDTO = (TokenResourceDTO) o;
    return Objects.equals(this.accessToken, tokenResourceDTO.accessToken) &&
        Objects.equals(this.tokenType, tokenResourceDTO.tokenType) &&
        Objects.equals(this.expiresIn, tokenResourceDTO.expiresIn) &&
        Objects.equals(this.refreshToken, tokenResourceDTO.refreshToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, tokenType, expiresIn, refreshToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TokenResourceDTO {\n");
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    expiresIn: ").append(toIndentedString(expiresIn)).append("\n");
    sb.append("    refreshToken: ").append(toIndentedString(refreshToken)).append("\n");
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

