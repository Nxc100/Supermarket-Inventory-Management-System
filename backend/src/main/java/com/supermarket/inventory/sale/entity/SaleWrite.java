package com.supermarket.inventory.sale.entity;

import com.supermarket.inventory.sale.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleWrite {
  private Long id;
  private final String orderNo, requestKey, requestHash, cashierName;
  private final long cashierId, totalQty;
  private final BigDecimal totalAmount, receivedAmount;
  private final PaymentMethod paymentMethod;
  private final LocalDateTime now;

  public SaleWrite(
      String orderNo,
      String requestKey,
      String requestHash,
      long cashierId,
      String cashierName,
      long totalQty,
      BigDecimal totalAmount,
      PaymentMethod paymentMethod,
      BigDecimal receivedAmount,
      LocalDateTime now) {
    this.orderNo = orderNo;
    this.requestKey = requestKey;
    this.requestHash = requestHash;
    this.cashierId = cashierId;
    this.cashierName = cashierName;
    this.totalQty = totalQty;
    this.totalAmount = totalAmount;
    this.paymentMethod = paymentMethod;
    this.receivedAmount = receivedAmount;
    this.now = now;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public String getCashierName() {
    return cashierName;
  }

  public long getCashierId() {
    return cashierId;
  }

  public long getTotalQty() {
    return totalQty;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public BigDecimal getReceivedAmount() {
    return receivedAmount;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public LocalDateTime getNow() {
    return now;
  }
}
