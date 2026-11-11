package com.supermarket.inventory.statistics.vo;

import com.supermarket.inventory.inventory.vo.StockInventoryVO;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record InventoryStatisticsVO(
    List<StockInventoryVO> records,
    long total,
    int pageNum,
    int pageSize,
    LocalDate businessDate,
    LocalDateTime collectedAt,
    Totals totals,
    List<CategoryRow> categories,
    Filters filters) {
  public record Filters(String keyword, String categoryId, String productId, Boolean enabled) {}

  public record Totals(
      long bookQty,
      long saleableQty,
      long expiredQty,
      long lowProductCount,
      long nearBatchCount,
      long expiredBatchCount) {}

  public record CategoryRow(
      String categoryId, String categoryName, long bookQty, BigDecimal percentage) {}
}
