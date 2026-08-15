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
 * 註冊請求體（UC-1）。密碼僅存在於此請求的短暫處理中，**不得持久化、記錄或回傳**（&#x60;FR-AUTH-06&#x60;）； 系統以不可逆方式雜湊後儲存。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthRegisterRequestDTO {

  private String username;

  private String email;

  private String password;

  public PostAuthRegisterRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthRegisterRequestDTO(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  public PostAuthRegisterRequestDTO username(String username) {
    this.username = username;
    return this;
  }

  /**
   * 登入帳號名，全系統唯一（對應 `users.username` UNIQUE）。
   * @return username
   */
  @NotNull @Size(min = 1, max = 64) 
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public PostAuthRegisterRequestDTO email(String email) {
    this.email = email;
    return this;
  }

  /**
   * 電子郵件，全系統唯一（對應 `users.email` UNIQUE）。格式非法 → `400`（`A0103`）。
   * @return email
   */
  @NotNull @Size(max = 255) @jakarta.validation.constraints.Email 
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public PostAuthRegisterRequestDTO password(String password) {
    this.password = password;
    return this;
  }

  /**
   * 明文密碼（僅用於本次雜湊，不落盤、不回傳）。
   * @return password
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostAuthRegisterRequestDTO postAuthRegisterRequestDTO = (PostAuthRegisterRequestDTO) o;
    return Objects.equals(this.username, postAuthRegisterRequestDTO.username) &&
        Objects.equals(this.email, postAuthRegisterRequestDTO.email) &&
        Objects.equals(this.password, postAuthRegisterRequestDTO.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, email, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthRegisterRequestDTO {\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: ").append("*").append("\n");
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

