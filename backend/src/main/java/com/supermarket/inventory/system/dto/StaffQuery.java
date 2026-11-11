package com.supermarket.inventory.system.dto;

import com.supermarket.inventory.common.PageQuery;
import com.supermarket.inventory.system.enums.StaffRole;

public class StaffQuery extends PageQuery {
  private StaffRole role;

  public StaffRole getRole() {
    return role;
  }

  public void setRole(StaffRole value) {
    role = value;
  }
}
