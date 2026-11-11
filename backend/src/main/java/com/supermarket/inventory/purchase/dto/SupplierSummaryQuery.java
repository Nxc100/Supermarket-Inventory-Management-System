package com.supermarket.inventory.purchase.dto;

import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class SupplierSummaryQuery extends PageQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String supplierId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  public String getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(String v) {
    supplierId = v == null || v.isBlank() ? null : v;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate v) {
    startDate = v;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate v) {
    endDate = v;
  }

  @AssertTrue(message = "结束日期不能早于开始日期")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
