package com.supermarket.inventory.inventory.dto;

public class StockQuery extends InventoryQuery {
  private Boolean lowStock;

  public Boolean getLowStock() {
    return lowStock;
  }

  public void setLowStock(Boolean value) {
    lowStock = value;
  }
}
