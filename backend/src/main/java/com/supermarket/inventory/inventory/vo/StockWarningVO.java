package com.supermarket.inventory.inventory.vo;

import java.time.LocalDate;

/** Operational projection; no costs, suppliers, owner identity or task IDs. */
public record StockWarningVO(
    String productId,
    String code,
    String name,
    String categoryName,
    String unit,
    boolean enabled,
    boolean canPurchase,
    boolean canSell,
    boolean frozen,
    long bookQty,
    long saleableQty,
    long stockLowerLimit,
    int nearExpiryDays,
    String batchItemId,
    String batchNo,
    LocalDate productionDate,
    LocalDate expiryDate,
    Integer remainingDays,
    long quantity) {}
