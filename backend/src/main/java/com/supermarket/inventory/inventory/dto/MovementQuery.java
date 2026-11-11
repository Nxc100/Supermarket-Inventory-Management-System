package com.supermarket.inventory.inventory.dto;

import com.supermarket.inventory.common.PageQuery;
import com.supermarket.inventory.inventory.enums.MovementType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class MovementQuery extends PageQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String productId;

  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String batchId;

  private MovementType eventType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String value) {
    productId = value == null || value.isBlank() ? null : value;
  }

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String value) {
    batchId = value == null || value.isBlank() ? null : value;
  }

  public MovementType getEventType() {
    return eventType;
  }

  public void setEventType(MovementType value) {
    eventType = value;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate value) {
    startDate = value;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate value) {
    endDate = value;
  }

  @AssertTrue(message = "结束日期不能早于开始日期")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
