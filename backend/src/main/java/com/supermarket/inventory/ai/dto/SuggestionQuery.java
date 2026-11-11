package com.supermarket.inventory.ai.dto;

import com.supermarket.inventory.ai.enums.*;
import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class SuggestionQuery extends PageQuery {
  private SuggestionType type;
  private SuggestionStatus status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  public SuggestionType getType() {
    return type;
  }

  public void setType(SuggestionType v) {
    type = v;
  }

  public SuggestionStatus getStatus() {
    return status;
  }

  public void setStatus(SuggestionStatus v) {
    status = v;
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
