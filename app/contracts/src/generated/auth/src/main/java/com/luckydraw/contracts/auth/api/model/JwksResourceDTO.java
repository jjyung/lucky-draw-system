package com.luckydraw.contracts.auth.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.luckydraw.contracts.auth.api.model.JwkResourceDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * JWKS（RFC 7517）承載，供公開取得簽章公鑰（&#x60;auth-keys-001&#x60;，AC-AUTH-008）。 支援多 key 輪替。僅含 public key；private key 存於 GCP Secret Manager（ADR-008），不可經任何端點取得（AC-AUTH-007）。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class JwksResourceDTO {

  @Valid
  private List<@Valid JwkResourceDTO> keys = new ArrayList<>();

  public JwksResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JwksResourceDTO(List<@Valid JwkResourceDTO> keys) {
    this.keys = keys;
  }

  public JwksResourceDTO keys(List<@Valid JwkResourceDTO> keys) {
    this.keys = keys;
    return this;
  }

  public JwksResourceDTO addKeysItem(JwkResourceDTO keysItem) {
    if (this.keys == null) {
      this.keys = new ArrayList<>();
    }
    this.keys.add(keysItem);
    return this;
  }

  /**
   * 公鑰清單（RSA）。驗證方以 `kid` 對應 JWT header 之 `kid` 選擇公鑰。
   * @return keys
   */
  @NotNull @Valid 
  @JsonProperty("keys")
  public List<@Valid JwkResourceDTO> getKeys() {
    return keys;
  }

  public void setKeys(List<@Valid JwkResourceDTO> keys) {
    this.keys = keys;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JwksResourceDTO jwksResourceDTO = (JwksResourceDTO) o;
    return Objects.equals(this.keys, jwksResourceDTO.keys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keys);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JwksResourceDTO {\n");
    sb.append("    keys: ").append(toIndentedString(keys)).append("\n");
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

