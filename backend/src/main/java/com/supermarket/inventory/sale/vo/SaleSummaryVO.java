package com.supermarket.inventory.sale.vo;

import com.supermarket.inventory.sale.entity.SaleOrder;
import com.supermarket.inventory.sale.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummaryVO(
    String id,
    String orderNo,
    String cashierId,
    String cashierName,
    SaleStatus status,
    int itemCount,
    long totalQty,
    @Schema(type = "string", example = "60.00") BigDecimal totalAmount,
    PaymentMethod paymentMethod,
    @Schema(type = "string") BigDecimal receivedAmount,
    @Schema(type = "string") BigDecimal changeAmount,
    LocalDateTime completedAt,
    LocalDateTime correctedAt,
    long rowVersion) {
  public static SaleSummaryVO from(SaleOrder o, int count) {
    return new SaleSummaryVO(
        o.id().toString(),
        o.orderNo(),
        o.cashierId().toString(),
        o.cashierName(),
        o.status(),
        count,
        o.totalQty(),
        o.totalAmount(),
        o.paymentMethod(),
        o.receivedAmount(),
        o.changeAmount(),
        o.completedAt(),
        o.correctedAt(),
        o.rowVersion());
  }
}
