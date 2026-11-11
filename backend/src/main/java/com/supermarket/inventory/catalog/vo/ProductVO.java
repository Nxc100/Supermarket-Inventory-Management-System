package com.supermarket.inventory.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Administrator archive projection; never reused for cashier responses. */
public record ProductVO(
    String id,
    String code,
    String barcode,
    String name,
    String categoryId,
    String categoryName,
    String specification,
    String unit,
    @Schema(type = "string", example = "3.00") BigDecimal referenceCost,
    @Schema(type = "string", example = "5.00") BigDecimal salePrice,
    String imagePath,
    boolean managesExpiry,
    Integer shelfLifeDays,
    int nearExpiryDays,
    long stockLowerLimit,
    boolean canPurchase,
    boolean canSell,
    boolean enabled,
    long stockQty,
    boolean referenced,
    String remark,
    LocalDateTime createdAt) {}
