package com.luckydraw.contracts.inventory.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 扣減／預留記錄生命週期狀態（`reservations.status`，SA §4.3 / inventory-db.md §3.4）。 **非事件 payload**，僅作為消費流程狀態機之輔助說明。  生命週期（單向，終態不可回轉）：  ```text RESERVED ──► COMMITTED  (扣減成功，終態)     │     └───────► REVERSED   (終態：庫存不足補償 UC-2 / 超時回收 UC-3) ```  - `RESERVED`：已確認、等待扣減（非終態；`reserved_at` 為超時回收基準，`FR-INV-05`） - `COMMITTED`：扣減成功（終態；`committed_at` 必有值） - `REVERSED`：已撤銷（終態；庫存不足補償 UC-2 或超時回收 UC-3） 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.9.0")
public enum ReservationStateEnum {
  
  RESERVED("RESERVED"),
  
  COMMITTED("COMMITTED"),
  
  REVERSED("REVERSED");

  private String value;

  ReservationStateEnum(String value) {
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
  public static ReservationStateEnum fromValue(String value) {
    for (ReservationStateEnum b : ReservationStateEnum.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

