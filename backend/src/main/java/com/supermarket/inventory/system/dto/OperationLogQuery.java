package com.supermarket.inventory.system.dto;

import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class OperationLogQuery extends PageQuery {
  @Size(max = 100)
  private String actor;

  @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}")
  private String action;

  @Pattern(regexp = "SUCCESS|FAILURE")
  private String result;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  public String getActor() {
    return actor;
  }

  public void setActor(String v) {
    actor = v == null || v.isBlank() ? null : v.strip();
  }

  public String getAction() {
    return action;
  }

  public void setAction(String v) {
    action = v == null || v.isBlank() ? null : v.strip();
  }

  public String getResult() {
    return result;
  }

  public void setResult(String v) {
    result = v == null || v.isBlank() ? null : v.strip();
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

  @AssertTrue(message = "请选择有效的开始和结束日期")
  public boolean isDateRangeValid() {
    return (startDate == null || startDate.getYear() >= 1 && startDate.getYear() <= 9998)
        && (endDate == null || endDate.getYear() >= 1 && endDate.getYear() <= 9998)
        && (startDate == null || endDate == null || !endDate.isBefore(startDate));
  }
}
