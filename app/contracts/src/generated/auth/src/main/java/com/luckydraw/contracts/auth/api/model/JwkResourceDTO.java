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
 * 單一 RSA 公鑰（RFC 7517 JWK，&#x60;kty&#x3D;RSA&#x60;）。&#x60;n&#x60;/&#x60;e&#x60; 為 base64url 編碼的模數與指數。 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public class JwkResourceDTO {

  private String kty;

  private String kid;

  private String use;

  private String alg;

  private String n;

  private String e;

  public JwkResourceDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JwkResourceDTO(String kty, String kid, String alg, String n, String e) {
    this.kty = kty;
    this.kid = kid;
    this.alg = alg;
    this.n = n;
    this.e = e;
  }

  public JwkResourceDTO kty(String kty) {
    this.kty = kty;
    return this;
  }

  /**
   * key type，固定 `RSA`。
   * @return kty
   */
  @NotNull 
  @JsonProperty("kty")
  public String getKty() {
    return kty;
  }

  public void setKty(String kty) {
    this.kty = kty;
  }

  public JwkResourceDTO kid(String kid) {
    this.kid = kid;
    return this;
  }

  /**
   * key id（用於多 key 輪替時讓驗證方選鑰，對應 JWT header `kid`）。
   * @return kid
   */
  @NotNull 
  @JsonProperty("kid")
  public String getKid() {
    return kid;
  }

  public void setKid(String kid) {
    this.kid = kid;
  }

  public JwkResourceDTO use(String use) {
    this.use = use;
    return this;
  }

  /**
   * 用途，固定 `sig`（簽章用）。
   * @return use
   */
  
  @JsonProperty("use")
  public String getUse() {
    return use;
  }

  public void setUse(String use) {
    this.use = use;
  }

  public JwkResourceDTO alg(String alg) {
    this.alg = alg;
    return this;
  }

  /**
   * 簽章演算法，固定 `RS256`。
   * @return alg
   */
  @NotNull 
  @JsonProperty("alg")
  public String getAlg() {
    return alg;
  }

  public void setAlg(String alg) {
    this.alg = alg;
  }

  public JwkResourceDTO n(String n) {
    this.n = n;
    return this;
  }

  /**
   * RSA modulus（base64url 編碼）。
   * @return n
   */
  @NotNull 
  @JsonProperty("n")
  public String getN() {
    return n;
  }

  public void setN(String n) {
    this.n = n;
  }

  public JwkResourceDTO e(String e) {
    this.e = e;
    return this;
  }

  /**
   * RSA public exponent（base64url 編碼，通常 `AQAB` = 65537）。
   * @return e
   */
  @NotNull 
  @JsonProperty("e")
  public String getE() {
    return e;
  }

  public void setE(String e) {
    this.e = e;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JwkResourceDTO jwkResourceDTO = (JwkResourceDTO) o;
    return Objects.equals(this.kty, jwkResourceDTO.kty) &&
        Objects.equals(this.kid, jwkResourceDTO.kid) &&
        Objects.equals(this.use, jwkResourceDTO.use) &&
        Objects.equals(this.alg, jwkResourceDTO.alg) &&
        Objects.equals(this.n, jwkResourceDTO.n) &&
        Objects.equals(this.e, jwkResourceDTO.e);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kty, kid, use, alg, n, e);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JwkResourceDTO {\n");
    sb.append("    kty: ").append(toIndentedString(kty)).append("\n");
    sb.append("    kid: ").append(toIndentedString(kid)).append("\n");
    sb.append("    use: ").append(toIndentedString(use)).append("\n");
    sb.append("    alg: ").append(toIndentedString(alg)).append("\n");
    sb.append("    n: ").append(toIndentedString(n)).append("\n");
    sb.append("    e: ").append(toIndentedString(e)).append("\n");
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

