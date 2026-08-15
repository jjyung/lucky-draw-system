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
 * 刷新請求體（UC-3）。以 &#x60;refreshToken&#x60; 換發新的存取憑證（Should，&#x60;FR-AUTH-04&#x60;）。 刷新僅延長有效期，不改變身份或權限。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthRefreshRequestDTO {

  private String refreshToken;

  public PostAuthRefreshRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthRefreshRequestDTO(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public PostAuthRefreshRequestDTO refreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

  /**
   * 登入時簽發的刷新憑證（JWT）。過期/無效 → `401`（`A0202`/`A0203`）。
   * @return refreshToken
   */
  @NotNull 
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
    PostAuthRefreshRequestDTO postAuthRefreshRequestDTO = (PostAuthRefreshRequestDTO) o;
    return Objects.equals(this.refreshToken, postAuthRefreshRequestDTO.refreshToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refreshToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthRefreshRequestDTO {\n");
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

