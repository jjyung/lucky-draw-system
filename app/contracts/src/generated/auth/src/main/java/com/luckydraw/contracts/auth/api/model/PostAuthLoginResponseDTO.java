package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.auth.api.model.TokenResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 登入成功回應 envelope。&#x60;data&#x60; 承載 &#x60;TokenResourceDTO&#x60;（含 refreshToken，Should）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthLoginResponseDTO {

  private String code;

  private String message;

  private TokenResourceDTO data;

  public PostAuthLoginResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthLoginResponseDTO(String code, String message, TokenResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public PostAuthLoginResponseDTO code(String code) {
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

  public PostAuthLoginResponseDTO message(String message) {
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

  public PostAuthLoginResponseDTO data(TokenResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public TokenResourceDTO getData() {
    return data;
  }

  public void setData(TokenResourceDTO data) {
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
    PostAuthLoginResponseDTO postAuthLoginResponseDTO = (PostAuthLoginResponseDTO) o;
    return Objects.equals(this.code, postAuthLoginResponseDTO.code) &&
        Objects.equals(this.message, postAuthLoginResponseDTO.message) &&
        Objects.equals(this.data, postAuthLoginResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthLoginResponseDTO {\n");
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

