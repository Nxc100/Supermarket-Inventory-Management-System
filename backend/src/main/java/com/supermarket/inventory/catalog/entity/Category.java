package com.supermarket.inventory.catalog.entity;

/** Mutable persistence model only, used to obtain MySQL generated keys. */
public class Category {
  private Long id;
  private String code;
  private String name;
  private int sortOrder;
  private boolean enabled;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean value) {
    enabled = value;
  }

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

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int value) {
    sortOrder = value;
  }
}
