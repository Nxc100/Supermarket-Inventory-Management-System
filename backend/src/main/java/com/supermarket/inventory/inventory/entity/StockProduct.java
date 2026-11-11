package com.supermarket.inventory.inventory.entity;

/**
 * Locked stock row. Only InventoryKernel changes balance/version fields. Never exposed as a
 * response.
 */
public class StockProduct {
  private Long id;
  private String code;
  private String barcode;
  private String name;
  private Long categoryId;
  private String specification;
  private String unit;
  private boolean managesExpiry;
  private boolean enabled;
  private boolean canPurchase;
  private boolean canSell;
  private java.math.BigDecimal salePrice;
  private long stockQty;
  private Long frozenStocktakeId;
  private long stockVersion;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    code = value;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String value) {
    barcode = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long value) {
    categoryId = value;
  }

  public String getSpecification() {
    return specification;
  }

  public void setSpecification(String value) {
    specification = value;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String value) {
    unit = value;
  }

  public boolean getManagesExpiry() {
    return managesExpiry;
  }

  public void setManagesExpiry(boolean value) {
    managesExpiry = value;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean value) {
    enabled = value;
  }

  public boolean getCanPurchase() {
    return canPurchase;
  }

  public void setCanPurchase(boolean value) {
    canPurchase = value;
  }

  public boolean getCanSell() {
    return canSell;
  }

  public void setCanSell(boolean value) {
    canSell = value;
  }

  public java.math.BigDecimal getSalePrice() {
    return salePrice;
  }

  public void setSalePrice(java.math.BigDecimal value) {
    salePrice = value;
  }

  public long getStockQty() {
    return stockQty;
  }

  public void setStockQty(long value) {
    stockQty = value;
  }

  public Long getFrozenStocktakeId() {
    return frozenStocktakeId;
  }

  public void setFrozenStocktakeId(Long value) {
    frozenStocktakeId = value;
  }

  public long getStockVersion() {
    return stockVersion;
  }

  public void setStockVersion(long value) {
    stockVersion = value;
  }
}
