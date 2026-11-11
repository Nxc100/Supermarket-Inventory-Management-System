package com.supermarket.inventory.purchase.vo;

import com.supermarket.inventory.purchase.entity.PurchaseOrder;
import com.supermarket.inventory.purchase.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.*;

public record PurchaseSummaryVO(
    String id,
    String orderNo,
    String supplierId,
    String supplierName,
    String operatorId,
    String operatorName,
    PurchaseStatus status,
    long totalQty,
    long totalReceivedQty,
    @Schema(type = "string", example = "60.00") BigDecimal totalAmount,
    ReconcileStatus reconcileStatus,
    long rowVersion,
    int reconcileVersion,
    boolean everSubmitted,
    LocalDate expectedArrivalDate,
    LocalDateTime arrivedAt,
    LocalDateTime submittedAt,
    LocalDateTime stockedAt,
    LocalDateTime createdAt,
    String remark) {
  public static PurchaseSummaryVO from(PurchaseOrder o) {
    return new PurchaseSummaryVO(
        o.id().toString(),
        o.orderNo(),
        o.supplierId().toString(),
        o.supplierName(),
        o.operatorId().toString(),
        o.operatorName(),
        o.status(),
        o.totalQty(),
        o.totalReceivedQty(),
        o.totalAmount(),
        o.reconcileStatus(),
        o.rowVersion(),
        o.reconcileVersion(),
        o.firstSubmittedAt() != null,
        o.expectedArrivalDate(),
        o.arrivedAt(),
        o.submittedAt(),
        o.stockedAt(),
        o.createdAt(),
        o.remark());
  }
}
