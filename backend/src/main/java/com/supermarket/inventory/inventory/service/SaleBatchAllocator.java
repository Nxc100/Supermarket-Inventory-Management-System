package com.supermarket.inventory.inventory.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.inventory.entity.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Pure allocation plan. The command must obtain product and batch locks before using it to write.
 */
public final class SaleBatchAllocator {
  private SaleBatchAllocator() {}

  public record Take(BatchInventory batch, int quantity) {}

  public static List<Take> allocate(
      Map<Long, StockProduct> products,
      List<BatchInventory> batches,
      Map<Long, StockReceipt> receipts,
      Map<Long, Integer> demands,
      LocalDate today) {
    List<Take> plan = new ArrayList<>();
    for (long id : demands.keySet().stream().sorted().toList()) {
      StockProduct p = products.get(id);
      if (p == null) throw BusinessException.notFound();
      if (!p.getEnabled() || !p.getCanSell())
        throw BusinessException.conflict("商品「" + p.getName() + "」已停用或禁止销售");
      if (p.getFrozenStocktakeId() != null)
        throw BusinessException.conflict("商品「" + p.getName() + "」存在盘点冻结，暂不可销售");
      int required = demands.get(id);
      if (required < 1 || required > 999999) throw BusinessException.conflict("销售数量超出范围");
      var eligible =
          batches.stream()
              .filter(
                  b ->
                      b.productId() == id
                          && b.batchNo() != null
                          && b.stockQty() != null
                          && b.stockQty() > 0)
              .filter(
                  b ->
                      receipts.containsKey(b.purchaseId())
                          && "IN_STOCK".equals(receipts.get(b.purchaseId()).status()))
              .filter(
                  b ->
                      !b.managesExpiry()
                          || b.expiryDate() != null && !b.expiryDate().isBefore(today))
              .sorted(
                  Comparator.comparing(
                          (BatchInventory b) -> b.managesExpiry() ? b.expiryDate() : LocalDate.MAX)
                      .thenComparing(b -> receipts.get(b.purchaseId()).stockedAt())
                      .thenComparing(BatchInventory::batchNo))
              .toList();
      long available = eligible.stream().mapToLong(BatchInventory::stockQty).sum();
      if (available < required)
        throw BusinessException.conflict(
            "商品「" + p.getName() + "」可售库存不足，当前可售" + available + p.getUnit());
      int remaining = required;
      for (var batch : eligible) {
        if (remaining == 0) break;
        int take = (int) Math.min(remaining, batch.stockQty());
        plan.add(new Take(batch, take));
        remaining -= take;
      }
    }
    return List.copyOf(plan);
  }
}
