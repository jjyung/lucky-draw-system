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
 * 登出成功回應 envelope。&#x60;data&#x60; 為 &#x60;null&#x60;（無資源承載）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthLogoutResponseDTO {

  private String code;

  private String message;

  private Object data = null;

  public PostAuthLogoutResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthLogoutResponseDTO(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public PostAuthLogoutResponseDTO code(String code) {
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

  public PostAuthLogoutResponseDTO message(String message) {
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

  public PostAuthLogoutResponseDTO data(Object data) {
    this.data = data;
    return this;
  }

  /**
   * 登出成功為 `null`。
   * @return data
   */
  
  @JsonProperty("data")
  public Object getData() {
    return data;
  }

  public void setData(Object data) {
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
    PostAuthLogoutResponseDTO postAuthLogoutResponseDTO = (PostAuthLogoutResponseDTO) o;
    return Objects.equals(this.code, postAuthLogoutResponseDTO.code) &&
        Objects.equals(this.message, postAuthLogoutResponseDTO.message) &&
        Objects.equals(this.data, postAuthLogoutResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthLogoutResponseDTO {\n");
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

