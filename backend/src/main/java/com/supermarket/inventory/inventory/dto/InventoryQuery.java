package com.supermarket.inventory.inventory.dto;

import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.Pattern;

public class InventoryQuery extends PageQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String categoryId;

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String value) {
    categoryId = value == null || value.isBlank() ? null : value;
  }
}
