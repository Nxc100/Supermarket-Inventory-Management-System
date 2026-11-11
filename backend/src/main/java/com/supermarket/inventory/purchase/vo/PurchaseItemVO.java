package com.supermarket.inventory.purchase.vo;

import com.supermarket.inventory.purchase.entity.PurchaseItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseItemVO(
    String id,
    int lineNo,
    String productId,
    String productCode,
    String productBarcode,
    String productName,
    String categoryId,
    String categoryName,
    String specification,
    String unit,
    boolean managesExpiry,
    int quantity,
    Integer receivedQty,
    @Schema(type = "string", example = "3.00") BigDecimal unitCost,
    @Schema(type = "string", example = "60.00") BigDecimal lineAmount,
    String supplierBatchNo,
    LocalDate productionDate,
    LocalDate expiryDate,
    String batchNo,
    Long stockQty,
    Integer externalQty,
    @Schema(type = "string", example = "3.00") BigDecimal externalUnitCost,
    @Schema(type = "string", example = "60.00") BigDecimal externalLineAmount,
    String reconcileNote) {
  public static PurchaseItemVO from(PurchaseItem i) {
    return new PurchaseItemVO(
        i.id().toString(),
        i.lineNo(),
        i.productId().toString(),
        i.productCode(),
        i.productBarcode(),
        i.productName(),
        i.categoryId().toString(),
        i.categoryName(),
        i.specification(),
        i.unit(),
        i.managesExpiry(),
        i.quantity(),
        i.receivedQty(),
        i.unitCost(),
        i.lineAmount(),
        i.supplierBatchNo(),
        i.productionDate(),
        i.expiryDate(),
        i.batchNo(),
        i.stockQty(),
        i.externalQty(),
        i.externalUnitCost(),
        i.externalLineAmount(),
        i.reconcileNote());
  }
}
