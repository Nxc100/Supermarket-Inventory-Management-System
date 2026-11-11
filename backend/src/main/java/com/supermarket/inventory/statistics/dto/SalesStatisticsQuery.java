package com.supermarket.inventory.statistics.dto;

import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class SalesStatisticsQuery extends PageQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String productId;

  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String categoryId;

  @Min(1)
  @Max(9998)
  private Integer year;

  @Min(1)
  @Max(3650)
  private int windowDays = 30;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String v) {
    productId = v == null || v.isBlank() ? null : v;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String v) {
    categoryId = v == null || v.isBlank() ? null : v;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer v) {
    year = v;
  }

  public int getWindowDays() {
    return windowDays;
  }

  public void setWindowDays(int v) {
    windowDays = v;
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
