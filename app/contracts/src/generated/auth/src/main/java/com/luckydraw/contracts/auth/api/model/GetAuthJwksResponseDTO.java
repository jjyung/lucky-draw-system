package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.auth.api.model.JwksResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * JWKS 回應 envelope。&#x60;data&#x60; 承載 &#x60;JwksResourceDTO&#x60;。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class GetAuthJwksResponseDTO {

  private String code;

  private String message;

  private JwksResourceDTO data;

  public GetAuthJwksResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetAuthJwksResponseDTO(String code, String message, JwksResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public GetAuthJwksResponseDTO code(String code) {
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

  public GetAuthJwksResponseDTO message(String message) {
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

  public GetAuthJwksResponseDTO data(JwksResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public JwksResourceDTO getData() {
    return data;
  }

  public void setData(JwksResourceDTO data) {
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
    GetAuthJwksResponseDTO getAuthJwksResponseDTO = (GetAuthJwksResponseDTO) o;
    return Objects.equals(this.code, getAuthJwksResponseDTO.code) &&
        Objects.equals(this.message, getAuthJwksResponseDTO.message) &&
        Objects.equals(this.data, getAuthJwksResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAuthJwksResponseDTO {\n");
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

