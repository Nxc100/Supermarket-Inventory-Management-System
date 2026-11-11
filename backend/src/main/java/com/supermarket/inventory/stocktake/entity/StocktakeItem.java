package com.supermarket.inventory.stocktake.entity;

import com.supermarket.inventory.stocktake.enums.CountReason;

public record StocktakeItem(
    Long id,
    Long stocktakeId,
    Long batchItemId,
    Long productId,
    long bookQty,
    Long actualQty,
    Long differenceQty,
    CountReason reasonType,
    String reasonNote) {}
