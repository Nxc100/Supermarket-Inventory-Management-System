package com.supermarket.inventory.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Strict safe projection: no costs, supplier, batch, balance or freeze-task identifiers. */
public record CashierInventoryVO(
    String id,
    String code,
    String barcode,
    String name,
    String categoryId,
    String categoryName,
    String specification,
    String unit,
    @Schema(type = "string", example = "5.00") BigDecimal salePrice,
    String imagePath,
    long saleableQty,
    boolean frozen) {}
