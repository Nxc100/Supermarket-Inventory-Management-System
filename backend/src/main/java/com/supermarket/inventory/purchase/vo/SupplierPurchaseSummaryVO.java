package com.supermarket.inventory.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record SupplierPurchaseSummaryVO(
    String supplierId,
    String supplierCode,
    String supplierName,
    long netQty,
    @Schema(type = "string", example = "60.00") BigDecimal netAmount,
    long unreconciledCount,
    long differenceCount) {}
