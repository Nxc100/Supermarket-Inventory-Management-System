package com.supermarket.inventory.ai.dto;

import com.supermarket.inventory.ai.enums.SuggestionType;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class AiUsageQuery {
  private LocalDate startDate, endDate;

  @Pattern(regexp = "qwen[a-zA-Z0-9.:-]{0,95}")
  private String model;

  private SuggestionType type;

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

  public String getModel() {
    return model;
  }

  public void setModel(String value) {
    model = value;
  }

  public SuggestionType getType() {
    return type;
  }

  public void setType(SuggestionType value) {
    type = value;
  }
}
