package com.supermarket.inventory.purchase.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseItem(
    Long id,
    Long purchaseId,
    int lineNo,
    Long productId,
    String productCode,
    String productBarcode,
    String productName,
    Long categoryId,
    String categoryName,
    String specification,
    String unit,
    boolean managesExpiry,
    int quantity,
    Integer receivedQty,
    BigDecimal unitCost,
    BigDecimal lineAmount,
    String supplierBatchNo,
    LocalDate productionDate,
    LocalDate expiryDate,
    String batchNo,
    Long stockQty,
    Integer externalQty,
    BigDecimal externalUnitCost,
    BigDecimal externalLineAmount,
    String reconcileNote) {}
