package com.supermarket.inventory.purchase.entity;

import java.math.BigDecimal;
import java.time.*;

/** Archive/total fields only; state and stock columns have explicit command SQL. */
public class PurchaseWrite {
  private Long id;
  private final String orderNo;
  private final long supplierId, operatorId, totalQty, totalReceivedQty;
  private final String supplierName,
      supplierContact,
      supplierPhone,
      supplierAddress,
      operatorName,
      remark;
  private final BigDecimal totalAmount;
  private final LocalDate expectedArrivalDate;
  private final LocalDateTime arrivedAt, now;

  public PurchaseWrite(
      String orderNo,
      long supplierId,
      String supplierName,
      String supplierContact,
      String supplierPhone,
      String supplierAddress,
      long operatorId,
      String operatorName,
      long totalQty,
      long totalReceivedQty,
      BigDecimal totalAmount,
      LocalDate expectedArrivalDate,
      LocalDateTime arrivedAt,
      String remark,
      LocalDateTime now) {
    this.orderNo = orderNo;
    this.supplierId = supplierId;
    this.supplierName = supplierName;
    this.supplierContact = supplierContact;
    this.supplierPhone = supplierPhone;
    this.supplierAddress = supplierAddress;
    this.operatorId = operatorId;
    this.operatorName = operatorName;
    this.totalQty = totalQty;
    this.totalReceivedQty = totalReceivedQty;
    this.totalAmount = totalAmount;
    this.expectedArrivalDate = expectedArrivalDate;
    this.arrivedAt = arrivedAt;
    this.remark = remark;
    this.now = now;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public long getSupplierId() {
    return supplierId;
  }

  public String getSupplierName() {
    return supplierName;
  }

  public String getSupplierContact() {
    return supplierContact;
  }

  public String getSupplierPhone() {
    return supplierPhone;
  }

  public String getSupplierAddress() {
    return supplierAddress;
  }

  public long getOperatorId() {
    return operatorId;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public long getTotalQty() {
    return totalQty;
  }

  public long getTotalReceivedQty() {
    return totalReceivedQty;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public LocalDate getExpectedArrivalDate() {
    return expectedArrivalDate;
  }

  public LocalDateTime getArrivedAt() {
    return arrivedAt;
  }

  public String getRemark() {
    return remark;
  }

  public LocalDateTime getNow() {
    return now;
  }
}
