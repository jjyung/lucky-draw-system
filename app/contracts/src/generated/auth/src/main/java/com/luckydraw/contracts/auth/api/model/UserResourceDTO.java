package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * 使用者資源（註冊成功回傳，UC-1 / AC-AUTH-001）。 **絕不含 &#x60;password&#x60; 或 &#x60;password_hash&#x60;**（&#x60;FR-AUTH-06&#x60;）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class UserResourceDTO {

  private Long id;

  private String username;

  private String email;

  /**
   * Gets or Sets roles
   */
  public enum RolesEnum {
    USER("ROLE_USER"),
    
    ADMIN("ROLE_ADMIN");

    private String value;

    RolesEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static RolesEnum fromValue(String value) {
      for (RolesEnum b : RolesEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @Valid
  private List<RolesEnum> roles = new ArrayList<>();

  public UserResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserResourceDTO(Long id, String username, String email, List<RolesEnum> roles) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.roles = roles;
  }

  public UserResourceDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * 使用者唯一識別（對應 `users.id`，BIGSERIAL；亦為 JWT `sub` claim 來源，ADR-009）。
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UserResourceDTO username(String username) {
    this.username = username;
    return this;
  }

  /**
   * 登入帳號名（全系統唯一）。
   * @return username
   */
  @NotNull 
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public UserResourceDTO email(String email) {
    this.email = email;
    return this;
  }

  /**
   * 電子郵件（全系統唯一）。
   * @return email
   */
  @NotNull @jakarta.validation.constraints.Email 
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserResourceDTO roles(List<RolesEnum> roles) {
    this.roles = roles;
    return this;
  }

  public UserResourceDTO addRolesItem(RolesEnum rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * 角色清單（授權依據）。註冊預設 `ROLE_USER`，使用者不得自選。
   * @return roles
   */
  @NotNull 
  @JsonProperty("roles")
  public List<RolesEnum> getRoles() {
    return roles;
  }

  public void setRoles(List<RolesEnum> roles) {
    this.roles = roles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserResourceDTO userResourceDTO = (UserResourceDTO) o;
    return Objects.equals(this.id, userResourceDTO.id) &&
        Objects.equals(this.username, userResourceDTO.username) &&
        Objects.equals(this.email, userResourceDTO.email) &&
        Objects.equals(this.roles, userResourceDTO.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, email, roles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserResourceDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
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

