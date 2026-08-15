package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.auth.api.model.AccessTokenResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 刷新成功回應 envelope。&#x60;data&#x60; 承載 &#x60;AccessTokenResourceDTO&#x60;（身份/角色不變）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthRefreshResponseDTO {

  private String code;

  private String message;

  private AccessTokenResourceDTO data;

  public PostAuthRefreshResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthRefreshResponseDTO(String code, String message, AccessTokenResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public PostAuthRefreshResponseDTO code(String code) {
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

  public PostAuthRefreshResponseDTO message(String message) {
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

  public PostAuthRefreshResponseDTO data(AccessTokenResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public AccessTokenResourceDTO getData() {
    return data;
  }

  public void setData(AccessTokenResourceDTO data) {
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
    PostAuthRefreshResponseDTO postAuthRefreshResponseDTO = (PostAuthRefreshResponseDTO) o;
    return Objects.equals(this.code, postAuthRefreshResponseDTO.code) &&
        Objects.equals(this.message, postAuthRefreshResponseDTO.message) &&
        Objects.equals(this.data, postAuthRefreshResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthRefreshResponseDTO {\n");
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

