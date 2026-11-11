package com.supermarket.inventory.inventory.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.entity.*;
import com.supermarket.inventory.inventory.enums.MovementType;
import com.supermarket.inventory.inventory.mapper.InventoryCommandMapper;
import com.supermarket.inventory.security.CurrentUser;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * Only this component writes product/batch balances. Callers lock their document before entering.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class InventoryKernel {
  public static final long MAX_STOCK = 999999999999L;
  private final InventoryCommandMapper mapper;
  private final DocumentNumbers numbers;

  public InventoryKernel(InventoryCommandMapper mapper, DocumentNumbers numbers) {
    this.mapper = mapper;
    this.numbers = numbers;
  }

  public Map<Long, StockProduct> lockProducts(Collection<Long> ids) {
    List<Long> sorted = ids.stream().distinct().sorted().toList();
    if (sorted.isEmpty()) throw BusinessException.conflict("单据没有商品明细");
    var rows = mapper.lockProducts(sorted);
    if (rows.size() != sorted.size()) throw BusinessException.notFound();
    Map<Long, StockProduct> result = new LinkedHashMap<>();
    for (var row : rows) result.put(row.getId(), row);
    return result;
  }

  public List<BatchInventory> lockBatches(long purchaseId) {
    return mapper.lockPurchaseBatches(purchaseId);
  }

  public List<BatchInventory> lockSaleBatches(Collection<Long> productIds) {
    return mapper.lockSaleBatches(productIds.stream().distinct().sorted().toList());
  }

  public List<BatchInventory> lockStocktakeBatches(Collection<Long> productIds) {
    return mapper.lockStocktakeBatches(productIds.stream().distinct().sorted().toList());
  }

  public List<Long> frozenProductIds(long stocktakeId) {
    return mapper.frozenProductIds(stocktakeId);
  }

  public void freeze(Map<Long, StockProduct> products, long stocktakeId) {
    for (var p : products.values()) {
      unfrozen(p);
      if (mapper.freeze(p.getId(), null, stocktakeId) != 1)
        throw BusinessException.conflict("商品冻结状态已变化，请刷新");
      p.setFrozenStocktakeId(stocktakeId);
      p.setStockVersion(p.getStockVersion() + 1);
    }
  }

  public void release(Map<Long, StockProduct> products, long stocktakeId) {
    for (var p : products.values()) {
      ownFreeze(p, stocktakeId);
      if (mapper.freeze(p.getId(), stocktakeId, null) != 1)
        throw BusinessException.conflict("盘点冻结归属已变化，请核查任务");
      p.setFrozenStocktakeId(null);
      p.setStockVersion(p.getStockVersion() + 1);
    }
  }

  public void ownFreeze(StockProduct product, long stocktakeId) {
    if (!Long.valueOf(stocktakeId).equals(product.getFrozenStocktakeId()))
      throw BusinessException.conflict("商品「" + product.getName() + "」冻结不属于本盘点，请管理员核查后关闭重建");
  }

  /** Only a task holding the product freeze can adjust its original, still-existing batch. */
  public void adjustStocktake(
      StockProduct product,
      BatchInventory batch,
      long actual,
      long itemId,
      long stocktakeId,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now) {
    ownFreeze(product, stocktakeId);
    if (batch.stockQty() == null
        || !batch.productId().equals(product.getId())
        || actual < 0
        || actual > MAX_STOCK) throw BusinessException.conflict("盘点来源批次或实盘数量不合法");
    long before = batch.stockQty(), delta = actual - before;
    if (delta == 0) return;
    if (mapper.batchBalance(batch.id(), before, actual) != 1)
      throw BusinessException.conflict("批次快照已变化，请关闭重建盘点");
    record(
        product,
        batch,
        delta > 0 ? MovementType.STOCKTAKE_GAIN : MovementType.STOCKTAKE_LOSS,
        delta,
        before,
        actual,
        number,
        actor,
        reason,
        now,
        null,
        itemId,
        stocktakeId);
  }

  public List<BatchInventory> lockBatchIds(Collection<Long> ids) {
    var sorted = ids.stream().distinct().sorted().toList();
    if (sorted.isEmpty()) throw BusinessException.conflict("单据缺少来源批次");
    var batches = mapper.lockBatchIds(sorted);
    if (batches.size() != sorted.size()) throw BusinessException.conflict("来源批次已不存在，不能处理");
    return batches;
  }

  public Map<Long, StockReceipt> receipts(List<BatchInventory> batches) {
    if (batches.isEmpty()) return Map.of();
    return mapper
        .receipts(batches.stream().map(BatchInventory::purchaseId).distinct().sorted().toList())
        .stream()
        .collect(
            java.util.stream.Collectors.toMap(
                StockReceipt::id, java.util.function.Function.identity()));
  }

  public Map<Long, String> categories(Collection<Long> ids) {
    return mapper.categories(ids.stream().distinct().sorted().toList()).stream()
        .collect(java.util.stream.Collectors.toMap(StockCategory::id, StockCategory::name));
  }

  public void deductSale(
      StockProduct product,
      BatchInventory batch,
      int quantity,
      long allocationId,
      String number,
      CurrentUser actor,
      LocalDateTime now) {
    changeSale(product, batch, quantity, allocationId, number, actor, null, now, false);
  }

  public void restoreSale(
      StockProduct product,
      BatchInventory batch,
      int quantity,
      long allocationId,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now) {
    changeSale(product, batch, quantity, allocationId, number, actor, reason, now, true);
  }

  private void changeSale(
      StockProduct product,
      BatchInventory batch,
      int quantity,
      long allocationId,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now,
      boolean restore) {
    unfrozen(product);
    if (quantity < 1
        || batch.stockQty() == null
        || batch.batchNo() == null
        || !batch.productId().equals(product.getId()))
      throw BusinessException.conflict("来源批次或分配数量不合法");
    long before = batch.stockQty(), after = Math.addExact(before, restore ? quantity : -quantity);
    if (after < 0 || after > MAX_STOCK) throw BusinessException.conflict("批次库存余额超出范围");
    if (mapper.batchBalance(batch.id(), before, after) != 1)
      throw BusinessException.conflict("批次库存已变化，请查询本次结果后重试");
    record(
        product,
        batch,
        restore ? MovementType.SALE_CORRECTION : MovementType.SALE_OUT,
        restore ? quantity : -quantity,
        before,
        after,
        number,
        actor,
        reason,
        now,
        allocationId);
  }

  public void validateReceipt(
      Map<Long, StockProduct> products, List<BatchInventory> batches, LocalDate today) {
    if (batches.isEmpty()) throw BusinessException.conflict("采购单没有有效明细");
    for (int i = 0; i < batches.size(); i++) {
      var batch = batches.get(i);
      var product = products.get(batch.productId());
      String label = "第" + (i + 1) + "行（" + product.getName() + "）";
      availableForPurchase(product, label);
      if (batch.receivedQty() == null || batch.receivedQty() != batch.quantity())
        throw BusinessException.conflict(label + "实收数量必须等于采购数量");
      if (batch.managesExpiry() != product.getManagesExpiry())
        throw BusinessException.conflict(label + "保质期管理方式已变化，请修改草稿");
      validateDates(
          batch.managesExpiry(), batch.productionDate(), batch.expiryDate(), today, true, label);
      if (batch.batchNo() != null || batch.stockQty() != null)
        throw BusinessException.conflict(label + "已经建立库存批次，请刷新单据");
    }
  }

  private void availableForPurchase(StockProduct p, String label) {
    if (!p.getEnabled() || !p.getCanPurchase())
      throw BusinessException.conflict(label + "已停用或禁止采购，请修正单据");
    if (p.getFrozenStocktakeId() != null)
      throw BusinessException.conflict(label + "存在盘点冻结，请先完成或关闭盘点");
  }

  public void unfrozen(StockProduct p) {
    if (p.getFrozenStocktakeId() != null)
      throw BusinessException.conflict("商品「" + p.getName() + "」存在盘点冻结，请先完成或关闭盘点");
  }

  public void receive(
      Map<Long, StockProduct> products,
      List<BatchInventory> batches,
      String number,
      CurrentUser actor,
      LocalDateTime now) {
    validateReceipt(products, batches, now.toLocalDate());
    for (var batch : batches) {
      var product = products.get(batch.productId());
      long qty = batch.receivedQty();
      if (mapper.establishBatch(batch.id(), numbers.next("PC"), qty) != 1)
        throw BusinessException.conflict("批次已被处理，请刷新单据");
      record(product, batch, MovementType.PURCHASE_IN, qty, 0, qty, number, actor, null, now);
    }
  }

  public void reverse(
      Map<Long, StockProduct> products,
      List<BatchInventory> batches,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now) {
    if (batches.isEmpty()) throw BusinessException.conflict("采购单缺少来源批次，不能撤销");
    if (mapper.laterMovements(batches.stream().map(BatchInventory::id).toList()) > 0)
      throw BusinessException.conflict("来源批次已有销售、盘点或其他后续流水，不能撤销入库");
    for (var batch : batches) {
      var product = products.get(batch.productId());
      unfrozen(product);
      if (batch.stockQty() == null
          || batch.receivedQty() == null
          || batch.stockQty().longValue() != batch.receivedQty().longValue())
        throw BusinessException.conflict("批次当前数量不等于原始入库数量，不能撤销");
      long before = batch.stockQty();
      if (mapper.batchBalance(batch.id(), before, 0) != 1)
        throw BusinessException.conflict("批次余额已变化，请刷新");
      record(
          product,
          batch,
          MovementType.PURCHASE_REVERSE,
          -before,
          before,
          0,
          number,
          actor,
          reason,
          now);
    }
  }

  private void record(
      StockProduct product,
      BatchInventory batch,
      MovementType type,
      long delta,
      long batchBefore,
      long batchAfter,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now) {
    record(product, batch, type, delta, batchBefore, batchAfter, number, actor, reason, now, null);
  }

  private void record(
      StockProduct product,
      BatchInventory batch,
      MovementType type,
      long delta,
      long batchBefore,
      long batchAfter,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now,
      Long allocationId) {
    record(
        product,
        batch,
        type,
        delta,
        batchBefore,
        batchAfter,
        number,
        actor,
        reason,
        now,
        allocationId,
        null,
        null);
  }

  private void record(
      StockProduct product,
      BatchInventory batch,
      MovementType type,
      long delta,
      long batchBefore,
      long batchAfter,
      String number,
      CurrentUser actor,
      String reason,
      LocalDateTime now,
      Long allocationId,
      Long stocktakeItemId,
      Long freezeId) {
    if (freezeId == null) unfrozen(product);
    else ownFreeze(product, freezeId);
    long before = product.getStockQty(), after = Math.addExact(before, delta);
    if (after < 0 || after > MAX_STOCK)
      throw BusinessException.conflict("商品「" + product.getName() + "」库存余额超出允许范围");
    if (mapper.productBalance(product, after, freezeId) != 1)
      throw BusinessException.conflict("商品库存或冻结状态已变化，请刷新单据");
    try {
      if (mapper.movement(
              type,
              product.getId(),
              batch.id(),
              number,
              delta,
              batchBefore,
              batchAfter,
              before,
              after,
              actor,
              reason,
              now,
              allocationId,
              stocktakeItemId)
          != 1) throw new IllegalStateException("Movement insert failed");
    } catch (DuplicateKeyException e) {
      throw BusinessException.conflict("该库存业务已被处理，请刷新单据确认结果");
    }
    product.setStockQty(after);
    product.setStockVersion(product.getStockVersion() + 1);
  }

  public static void validateDates(
      boolean manages,
      LocalDate production,
      LocalDate expiry,
      LocalDate today,
      boolean complete,
      String label) {
    if (!manages) {
      if (production != null || expiry != null)
        throw BusinessException.conflict(label + "普通商品不得填写保质期日期");
      return;
    }
    if (complete && (production == null || expiry == null))
      throw BusinessException.conflict(label + "必须登记生产日期和到期日期");
    if (production != null && production.isAfter(today))
      throw BusinessException.conflict(label + "生产日期不能晚于当前日期");
    if (production != null && expiry != null && expiry.isBefore(production))
      throw BusinessException.conflict(label + "到期日期不能早于生产日期");
    if (expiry != null && expiry.isBefore(today))
      throw BusinessException.conflict(label + "已过期，不能提交或审核入库");
  }
}
