package com.supermarket.inventory.sale.entity;

import java.math.BigDecimal;

public record SaleQuoteProduct(
    Long id,
    String code,
    String name,
    String unit,
    BigDecimal salePrice,
    boolean enabled,
    boolean canSell,
    boolean frozen,
    long saleableQty) {}
