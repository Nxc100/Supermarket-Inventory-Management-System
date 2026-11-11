package com.supermarket.inventory.sale.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SaleDetailVO(
    SaleSummaryVO order,
    List<SaleItemVO> items,
    Correction correction,
    boolean canCorrectToday,
    boolean expiredSourceStock) {
  public record Correction(
      String actorId, String actorName, LocalDateTime occurredAt, String reason) {}
}
