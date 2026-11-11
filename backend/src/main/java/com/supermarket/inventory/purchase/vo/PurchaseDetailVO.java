package com.supermarket.inventory.purchase.vo;

import com.supermarket.inventory.purchase.entity.PurchaseOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseDetailVO(
    PurchaseSummaryVO order,
    SupplierSnapshot supplierSnapshot,
    List<PurchaseItemVO> items,
    Review review,
    EndInfo closure,
    EndInfo reversal,
    Reconciliation reconciliation,
    List<ReconcileHistoryVO> reconcileHistory) {
  public record SupplierSnapshot(String name, String contact, String phone, String address) {}

  public record Review(
      String reviewerId, String reviewerName, LocalDateTime reviewedAt, String rejectionReason) {}

  public record EndInfo(String actorId, LocalDateTime occurredAt, String reason) {}

  public record Reconciliation(
      @Schema(type = "string") BigDecimal externalTotal,
      String reference,
      String difference,
      String resolution,
      String reconcilerId,
      LocalDateTime reconciledAt) {}

  public static PurchaseDetailVO from(
      PurchaseOrder o, List<PurchaseItemVO> items, List<ReconcileHistoryVO> history) {
    return new PurchaseDetailVO(
        PurchaseSummaryVO.from(o),
        new SupplierSnapshot(
            o.supplierName(), o.supplierContact(), o.supplierPhone(), o.supplierAddress()),
        items,
        new Review(id(o.reviewedBy()), o.reviewerName(), o.reviewedAt(), o.rejectionReason()),
        new EndInfo(id(o.closedBy()), o.closedAt(), o.closeReason()),
        new EndInfo(id(o.reversedBy()), o.reversedAt(), o.reverseReason()),
        new Reconciliation(
            o.externalTotalAmount(),
            o.reconcileReference(),
            o.reconcileDifference(),
            o.reconcileResolution(),
            id(o.reconciledBy()),
            o.reconciledAt()),
        history);
  }

  private static String id(Long id) {
    return id == null ? null : id.toString();
  }
}
