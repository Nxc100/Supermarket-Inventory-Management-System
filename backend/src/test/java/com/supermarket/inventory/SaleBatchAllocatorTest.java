package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;

import com.supermarket.inventory.inventory.entity.*;
import com.supermarket.inventory.inventory.service.SaleBatchAllocator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SaleBatchAllocatorTest {
  private final LocalDate today = LocalDate.of(2026, 11, 10);

  private StockProduct product(boolean expiry) {
    var p = new StockProduct();
    p.setId(1L);
    p.setName("测试商品");
    p.setEnabled(true);
    p.setCanSell(true);
    p.setManagesExpiry(expiry);
    return p;
  }

  private BatchInventory batch(long id, int qty, LocalDate expiry) {
    return new BatchInventory(
        id,
        1L,
        1L,
        qty,
        qty,
        new BigDecimal("3.00"),
        expiry != null,
        today.minusDays(1),
        expiry,
        "BATCH-" + id,
        (long) qty);
  }

  private Map<Long, StockReceipt> receipts() {
    return Map.of(1L, new StockReceipt(1L, "IN_STOCK", today.minusDays(2).atStartOfDay()));
  }

  @Test
  void nearExpiryFirstAndOneLineCanSpanBatches() {
    var plan =
        SaleBatchAllocator.allocate(
            Map.of(1L, product(true)),
            List.of(batch(2, 10, today.plusDays(20)), batch(1, 10, today)),
            receipts(),
            Map.of(1L, 12),
            today);
    assertThat(plan).extracting(x -> x.batch().id()).containsExactly(1L, 2L);
    assertThat(plan).extracting(x -> x.quantity()).containsExactly(10, 2);
  }

  @Test
  void expiryTodayIsValidButPastStockCannotFillTheSale() {
    assertThatThrownBy(
            () ->
                SaleBatchAllocator.allocate(
                    Map.of(1L, product(true)),
                    List.of(batch(1, 5, today.minusDays(1)), batch(2, 2, today)),
                    receipts(),
                    Map.of(1L, 3),
                    today))
        .hasMessageContaining("可售库存不足");
  }

  @Test
  void ordinaryProductsUseReceiptTimeThenBatchNumberAndFreezeStillBlocks() {
    var ordinary = product(false);
    var older =
        new BatchInventory(2L, 2L, 1L, 4, 4, new BigDecimal("3.00"), false, null, null, "B", 4L);
    var headers = new HashMap<>(receipts());
    headers.put(2L, new StockReceipt(2L, "IN_STOCK", today.minusDays(3).atStartOfDay()));
    var plan =
        SaleBatchAllocator.allocate(
            Map.of(1L, ordinary), List.of(batch(1, 4, null), older), headers, Map.of(1L, 5), today);
    assertThat(plan).extracting(x -> x.batch().id()).containsExactly(2L, 1L);
    ordinary.setFrozenStocktakeId(2L);
    assertThatThrownBy(
            () ->
                SaleBatchAllocator.allocate(
                    Map.of(1L, ordinary), List.of(older), headers, Map.of(1L, 1), today))
        .hasMessageContaining("盘点冻结");
  }
}
