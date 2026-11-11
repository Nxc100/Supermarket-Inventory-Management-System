package com.supermarket.inventory.statistics.dto;

import com.supermarket.inventory.inventory.dto.InventoryQuery;
import jakarta.validation.constraints.Pattern;

public class InventoryStatisticsQuery extends InventoryQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String productId;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String v) {
    productId = v == null || v.isBlank() ? null : v;
  }
}
