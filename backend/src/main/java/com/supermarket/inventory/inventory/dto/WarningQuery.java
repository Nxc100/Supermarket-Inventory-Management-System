package com.supermarket.inventory.inventory.dto;

public class WarningQuery extends InventoryQuery {
  @jakarta.validation.constraints.Pattern(regexp = "[1-9][0-9]{0,18}")
  private String productId;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String v) {
    productId = v == null || v.isBlank() ? null : v;
  }

  public enum Type {
    LOW,
    NEAR,
    EXPIRED
  }

  private Type type = Type.LOW;

  public Type getType() {
    return type;
  }

  public void setType(Type value) {
    type = value == null ? Type.LOW : value;
  }
}
