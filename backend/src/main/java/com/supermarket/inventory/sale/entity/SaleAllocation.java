package com.supermarket.inventory.sale.entity;

public record SaleAllocation(
    Long id, Long saleItemId, Long productId, Long batchItemId, int quantity) {}
