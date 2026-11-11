package com.supermarket.inventory.catalog.dto;

import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.Pattern;

public class ProductQuery extends PageQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String categoryId;

  private Boolean managesExpiry;
  private Boolean canPurchase;
  private Boolean canSell;

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String value) {
    categoryId = value == null || value.isBlank() ? null : value;
  }

  public Boolean getManagesExpiry() {
    return managesExpiry;
  }

  public void setManagesExpiry(Boolean value) {
    managesExpiry = value;
  }

  public Boolean getCanPurchase() {
    return canPurchase;
  }

  public void setCanPurchase(Boolean value) {
    canPurchase = value;
  }

  public Boolean getCanSell() {
    return canSell;
  }

  public void setCanSell(Boolean value) {
    canSell = value;
  }
}
