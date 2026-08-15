package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.auth.api.model.UserResourceDTO;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 註冊成功回應 envelope。&#x60;data&#x60; 承載 &#x60;UserResourceDTO&#x60;（不含密碼/雜湊）。
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthRegisterResponseDTO {

  private String code;

  private String message;

  private UserResourceDTO data;

  public PostAuthRegisterResponseDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthRegisterResponseDTO(String code, String message, UserResourceDTO data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public PostAuthRegisterResponseDTO code(String code) {
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

  public PostAuthRegisterResponseDTO message(String message) {
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

  public PostAuthRegisterResponseDTO data(UserResourceDTO data) {
    this.data = data;
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @NotNull @Valid 
  @JsonProperty("data")
  public UserResourceDTO getData() {
    return data;
  }

  public void setData(UserResourceDTO data) {
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
    PostAuthRegisterResponseDTO postAuthRegisterResponseDTO = (PostAuthRegisterResponseDTO) o;
    return Objects.equals(this.code, postAuthRegisterResponseDTO.code) &&
        Objects.equals(this.message, postAuthRegisterResponseDTO.message) &&
        Objects.equals(this.data, postAuthRegisterResponseDTO.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthRegisterResponseDTO {\n");
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

