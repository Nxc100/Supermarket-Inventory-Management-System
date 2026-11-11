package com.supermarket.inventory.ai.dto;

import com.supermarket.inventory.ai.enums.SuggestionType;
import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.*;

public class SuggestionProductQuery extends PageQuery {
  @NotNull private SuggestionType type;

  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String categoryId;

  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String productId;

  public SuggestionType getType() {
    return type;
  }

  public void setType(SuggestionType v) {
    type = v;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String v) {
    categoryId = v == null || v.isBlank() ? null : v;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String v) {
    productId = v == null || v.isBlank() ? null : v;
  }
}
