package com.supermarket.inventory.inventory.vo;

import java.time.LocalDateTime;

public record StockInventoryVO(
    String id,
    String code,
    String barcode,
    String name,
    String categoryId,
    String categoryName,
    String specification,
    String unit,
    boolean enabled,
    boolean canSell,
    long bookQty,
    long batchQty,
    long saleableQty,
    long expiredQty,
    long stockLowerLimit,
    boolean frozen,
    LocalDateTime lastStockedAt) {}
