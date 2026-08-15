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
 * 錯誤回應 envelope（成功與失敗同一結構，&#x60;docs/api/README.md&#x60; §2.2）。 &#x60;code&#x60; 為數字錯誤碼字串（&#x60;Axxxx&#x60; 用戶端 / &#x60;Bxxxx&#x60; 系統 / &#x60;Cxxxx&#x60; 第三方）； HTTP status 與 &#x60;code&#x60; 的對映見 &#x60;docs/api/README.md&#x60; §2.3。&#x60;data&#x60; 失敗時為 &#x60;null&#x60;。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class ErrorEnvelope {

  private String code;

  private String message;

  private Object data = null;

  public ErrorEnvelope() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorEnvelope(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public ErrorEnvelope code(String code) {
    this.code = code;
    return this;
  }

  /**
   * 數字錯誤碼（如 `A0201`）。
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

  public ErrorEnvelope message(String message) {
    this.message = message;
    return this;
  }

  /**
   * 人類可讀錯誤訊息。
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

  public ErrorEnvelope data(Object data) {
    this.data = data;
    return this;
  }

  /**
   * 失敗時為 `null`。
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
    ErrorEnvelope errorEnvelope = (ErrorEnvelope) o;
    return Objects.equals(this.code, errorEnvelope.code) &&
        Objects.equals(this.message, errorEnvelope.message) &&
        Objects.equals(this.data, errorEnvelope.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorEnvelope {\n");
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

