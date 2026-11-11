package com.supermarket.inventory.sale.dto;

import com.supermarket.inventory.common.PageQuery;
import com.supermarket.inventory.sale.enums.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class SaleQuery extends PageQuery {
  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String productId;

  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String cashierId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  private PaymentMethod paymentMethod;
  private SaleStatus status;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String v) {
    productId = v == null || v.isBlank() ? null : v;
  }

  public String getCashierId() {
    return cashierId;
  }

  public void setCashierId(String v) {
    cashierId = v == null || v.isBlank() ? null : v;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate v) {
    startDate = v;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate v) {
    endDate = v;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod v) {
    paymentMethod = v;
  }

  public SaleStatus getStatus() {
    return status;
  }

  public void setStatus(SaleStatus v) {
    status = v;
  }

  @AssertTrue(message = "结束日期不能早于开始日期")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
