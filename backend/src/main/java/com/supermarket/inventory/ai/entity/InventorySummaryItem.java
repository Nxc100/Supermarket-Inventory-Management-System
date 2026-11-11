package com.supermarket.inventory.ai.entity;

import java.time.LocalDateTime;

public record InventorySummaryItem(
    String productId,
    String code,
    String name,
    String categoryName,
    String unit,
    long bookQty,
    long saleableQty,
    long stockLowerLimit,
    long recentNetQty,
    long nearQty,
    long expiredQty,
    long nearBatchCount,
    long expiredBatchCount,
    LocalDateTime lastStockedAt) {}
