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
 * 登入請求體（UC-2）。&#x60;username&#x60; 欄位可填 **username 或 email**（SA UC-2 Precondition）。 憑證承載內容（身份/角色/有效期/簽發時刻/簽發者）由伺服端決定，client 不得指定。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class PostAuthLoginRequestDTO {

  private String username;

  private String password;

  public PostAuthLoginRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PostAuthLoginRequestDTO(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public PostAuthLoginRequestDTO username(String username) {
    this.username = username;
    return this;
  }

  /**
   * 登入識別：username 或 email 皆可。
   * @return username
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public PostAuthLoginRequestDTO password(String password) {
    this.password = password;
    return this;
  }

  /**
   * 明文密碼（僅用於本次比對）。
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
    PostAuthLoginRequestDTO postAuthLoginRequestDTO = (PostAuthLoginRequestDTO) o;
    return Objects.equals(this.username, postAuthLoginRequestDTO.username) &&
        Objects.equals(this.password, postAuthLoginRequestDTO.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostAuthLoginRequestDTO {\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
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

