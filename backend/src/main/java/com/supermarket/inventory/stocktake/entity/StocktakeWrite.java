package com.supermarket.inventory.stocktake.entity;

import java.time.LocalDateTime;

/** New task fields only; stock and state transitions use explicit command SQL. */
public class StocktakeWrite {
  private Long id;
  private final String orderNo, operatorName, remark;
  private final long operatorId;
  private final LocalDateTime now;

  public StocktakeWrite(
      String orderNo, long operatorId, String operatorName, String remark, LocalDateTime now) {
    this.orderNo = orderNo;
    this.operatorId = operatorId;
    this.operatorName = operatorName;
    this.remark = remark;
    this.now = now;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public String getRemark() {
    return remark;
  }

  public long getOperatorId() {
    return operatorId;
  }

  public LocalDateTime getNow() {
    return now;
  }
}
