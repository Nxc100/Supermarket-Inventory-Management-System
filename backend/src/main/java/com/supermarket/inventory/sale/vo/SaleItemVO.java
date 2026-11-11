package com.supermarket.inventory.sale.vo;

import com.supermarket.inventory.sale.entity.SaleItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record SaleItemVO(
    String id,
    int lineNo,
    String productId,
    String productCode,
    String productBarcode,
    String productName,
    String categoryId,
    String categoryName,
    String specification,
    String unit,
    @Schema(type = "string") BigDecimal unitPrice,
    int quantity,
    @Schema(type = "string") BigDecimal lineAmount) {
  public static SaleItemVO from(SaleItem i) {
    return new SaleItemVO(
        i.id().toString(),
        i.lineNo(),
        i.productId().toString(),
        i.productCode(),
        i.productBarcode(),
        i.productName(),
        i.categoryId().toString(),
        i.categoryName(),
        i.specification(),
        i.unit(),
        i.unitPrice(),
        i.quantity(),
        i.lineAmount());
  }
}
