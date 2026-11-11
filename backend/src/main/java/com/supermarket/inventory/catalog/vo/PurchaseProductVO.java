package com.supermarket.inventory.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record PurchaseProductVO(
    String id,
    String code,
    String barcode,
    String name,
    String categoryId,
    String categoryName,
    String specification,
    String unit,
    @Schema(type = "string", example = "3.00") BigDecimal referenceCost,
    boolean managesExpiry,
    Integer shelfLifeDays,
    String imagePath,
    boolean frozen) {}
