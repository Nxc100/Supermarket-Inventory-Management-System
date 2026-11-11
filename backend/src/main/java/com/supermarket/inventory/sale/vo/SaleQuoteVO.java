package com.supermarket.inventory.sale.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleQuoteVO(
    boolean canConfirm,
    int itemCount,
    long totalQty,
    @Schema(type = "string") BigDecimal totalAmount,
    LocalDateTime checkedAt,
    List<Line> items) {
  public record Line(
      String productId,
      String productCode,
      String productName,
      String unit,
      int quantity,
      @Schema(type = "string") BigDecimal expectedPrice,
      @Schema(type = "string") BigDecimal currentPrice,
      @Schema(type = "string") BigDecimal lineAmount,
      long saleableQty,
      boolean priceChanged,
      String problem) {}
}
