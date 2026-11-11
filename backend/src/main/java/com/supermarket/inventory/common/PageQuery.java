package com.supermarket.inventory.common;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class PageQuery {
  @Min(1)
  private int pageNum = 1;

  private int pageSize = 10;

  @Size(max = 100)
  private String keyword = "";

  private Boolean enabled;

  @AssertTrue(message = "每页条数仅支持10、20、50")
  public boolean isPageSizeValid() {
    return pageSize == 10 || pageSize == 20 || pageSize == 50;
  }

  public int getPageNum() {
    return pageNum;
  }

  public void setPageNum(int value) {
    pageNum = value;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int value) {
    pageSize = value;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String value) {
    keyword = value == null ? "" : value.trim();
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean value) {
    enabled = value;
  }

  public long getOffset() {
    return (long) (pageNum - 1) * pageSize;
  }
}
