package com.supermarket.inventory.statistics.vo;

import java.time.*;
import java.util.List;

public record SlowProductPageVO(
    List<Row> records,
    long total,
    int pageNum,
    int pageSize,
    LocalDate windowStart,
    LocalDate windowEnd,
    int windowDays,
    LocalDateTime collectedAt) {
  public record Row(
      String productId,
      String code,
      String name,
      String unit,
      long saleableQty,
      LocalDateTime firstStockedAt,
      long netQty,
      String observation) {}
}
