package com.supermarket.inventory.catalog.dto;

import com.supermarket.inventory.common.PageQuery;
import jakarta.validation.constraints.Size;

public class SupplierQuery extends PageQuery {
  @Size(max = 30)
  private String contactName = "";

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String value) {
    contactName = value == null ? "" : value.trim();
  }
}
