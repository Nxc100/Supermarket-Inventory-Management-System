package com.supermarket.inventory.statistics.vo;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/** Every row is a shop-wide product aggregate; never a route to another cashier's document. */
public record SalesStatisticsVO(
    List<ProductRow> records,
    long total,
    int pageNum,
    int pageSize,
    int year,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime collectedAt,
    Totals totals,
    List<MonthRow> monthly,
    List<DayRow> daily,
    List<ProductRow> hotProducts) {
  public record Totals(
      long netQty, BigDecimal netAmount, long completedCount, long correctedCount) {}

  public record MonthRow(int month, long netQty, BigDecimal netAmount) {}

  public record DayRow(LocalDate date, long netQty, BigDecimal netAmount) {}

  public record ProductRow(
      String productId,
      String code,
      String name,
      String categoryId,
      String categoryName,
      String unit,
      long netQty,
      BigDecimal netAmount) {}
}
