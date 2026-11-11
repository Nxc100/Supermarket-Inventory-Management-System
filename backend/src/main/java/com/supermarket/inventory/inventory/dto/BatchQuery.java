package com.supermarket.inventory.inventory.dto;

import com.supermarket.inventory.common.PageQuery;

public class BatchQuery extends PageQuery {
  private boolean includeEmpty = true;

  public boolean isIncludeEmpty() {
    return includeEmpty;
  }

  public void setIncludeEmpty(boolean value) {
    includeEmpty = value;
  }
}
