package com.supermarket.inventory.sale.entity;

import java.math.BigDecimal;

public record SaleItem(
    Long id,
    Long saleId,
    int lineNo,
    Long productId,
    String productCode,
    String productBarcode,
    String productName,
    Long categoryId,
    String categoryName,
    String specification,
    String unit,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineAmount) {}
