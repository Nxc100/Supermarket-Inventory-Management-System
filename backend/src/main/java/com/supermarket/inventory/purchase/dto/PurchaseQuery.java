package com.supermarket.inventory.purchase.dto;

import com.supermarket.inventory.common.PageQuery;
import com.supermarket.inventory.purchase.enums.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class PurchaseQuery extends PageQuery {
  public enum DateBasis {
    CREATED,
    STOCKED,
    EVENT
  }

  @Pattern(regexp = "[1-9][0-9]{0,18}")
  private String supplierId;

  private PurchaseStatus status;
  private ReconcileStatus reconcileStatus;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  private DateBasis dateBasis = DateBasis.CREATED;

  public String getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(String v) {
    supplierId = v == null || v.isBlank() ? null : v;
  }

  public PurchaseStatus getStatus() {
    return status;
  }

  public void setStatus(PurchaseStatus v) {
    status = v;
  }

  public ReconcileStatus getReconcileStatus() {
    return reconcileStatus;
  }

  public void setReconcileStatus(ReconcileStatus v) {
    reconcileStatus = v;
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

  public DateBasis getDateBasis() {
    return dateBasis;
  }

  public void setDateBasis(DateBasis v) {
    dateBasis = v == null ? DateBasis.CREATED : v;
  }

  @AssertTrue(message = "结束日期不能早于开始日期")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
