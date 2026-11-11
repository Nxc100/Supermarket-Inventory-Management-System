package com.supermarket.inventory.catalog.entity;

/** Persistence projection restricted to archive-owned writable fields. */
public class Product {
  private Long id;
  private String code;
  private String barcode;
  private String name;
  private Long categoryId;
  private String specification;
  private String unit;
  private java.math.BigDecimal referenceCost;
  private java.math.BigDecimal salePrice;
  private boolean managesExpiry;
  private Integer shelfLifeDays;
  private int nearExpiryDays;
  private long stockLowerLimit;
  private boolean canPurchase;
  private boolean canSell;
  private String remark;
  private long createdBy;

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

  public java.math.BigDecimal getReferenceCost() {
    return referenceCost;
  }

  public void setReferenceCost(java.math.BigDecimal value) {
    referenceCost = value;
  }

  public java.math.BigDecimal getSalePrice() {
    return salePrice;
  }

  public void setSalePrice(java.math.BigDecimal value) {
    salePrice = value;
  }

  public boolean getManagesExpiry() {
    return managesExpiry;
  }

  public void setManagesExpiry(boolean value) {
    managesExpiry = value;
  }

  public Integer getShelfLifeDays() {
    return shelfLifeDays;
  }

  public void setShelfLifeDays(Integer value) {
    shelfLifeDays = value;
  }

  public int getNearExpiryDays() {
    return nearExpiryDays;
  }

  public void setNearExpiryDays(int value) {
    nearExpiryDays = value;
  }

  public long getStockLowerLimit() {
    return stockLowerLimit;
  }

  public void setStockLowerLimit(long value) {
    stockLowerLimit = value;
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

  public String getRemark() {
    return remark;
  }

  public void setRemark(String value) {
    remark = value;
  }

  public long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(long value) {
    createdBy = value;
  }
}
