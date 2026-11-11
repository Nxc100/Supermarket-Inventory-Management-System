package com.supermarket.inventory.sale.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.entity.*;
import com.supermarket.inventory.inventory.service.*;
import com.supermarket.inventory.sale.dto.*;
import com.supermarket.inventory.sale.entity.*;
import com.supermarket.inventory.sale.enums.*;
import com.supermarket.inventory.sale.mapper.SaleMapper;
import com.supermarket.inventory.sale.vo.SaleDetailVO;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class SaleCommandService {
  private final SaleMapper mapper;
  private final SaleAccess access;
  private final InventoryKernel inventory;
  private final SaleQueryService queries;
  private final AuditService audit;
  private final DocumentNumbers numbers;
  private final Clock clock;

  public SaleCommandService(
      SaleMapper mapper,
      SaleAccess access,
      InventoryKernel inventory,
      SaleQueryService queries,
      AuditService audit,
      DocumentNumbers numbers,
      Clock clock) {
    this.mapper = mapper;
    this.access = access;
    this.inventory = inventory;
    this.queries = queries;
    this.audit = audit;
    this.numbers = numbers;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public SaleDetailVO complete(SaleCompleteRequest request, String hash, CurrentUser user) {
    access.require(user, "sale.order.complete");
    var input = SaleInput.lines(request.items());
    var products = inventory.lockProducts(input.keySet());
    var existing = mapper.byKey(request.requestKey());
    if (existing != null) {
      access.sameRequest(existing, user, hash);
      return queries.detail(existing.id(), user);
    }
    var batches = inventory.lockSaleBatches(input.keySet());
    var receipts = inventory.receipts(batches);
    var now = LocalDateTime.now(clock);
    Map<Long, Integer> demands =
        input.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().quantity()));
    var plan = SaleBatchAllocator.allocate(products, batches, receipts, demands, now.toLocalDate());
    BigDecimal total = BigDecimal.ZERO;
    long quantity = 0;
    for (var entry : input.entrySet()) {
      var p = products.get(entry.getKey());
      var line = entry.getValue();
      if (p.getSalePrice().compareTo(line.expectedPrice()) != 0)
        throw new BusinessException(
            HttpStatus.CONFLICT,
            "PRICE_CHANGED",
            "商品「"
                + p.getName()
                + "」售价由"
                + line.expectedPrice().toPlainString()
                + "变为"
                + p.getSalePrice().toPlainString()
                + "，请核对后重新确认");
      var amount = p.getSalePrice().multiply(BigDecimal.valueOf(line.quantity()));
      SaleInput.bounded(amount);
      total = total.add(amount);
      quantity += line.quantity();
    }
    SaleInput.bounded(total);
    if (request.receivedAmount().compareTo(total) < 0) throw SaleInput.bad("实收金额不得少于应收金额");
    if (request.paymentMethod() == PaymentMethod.MOCK_EPAY
        && request.receivedAmount().compareTo(total) != 0)
      throw SaleInput.bad("模拟电子支付实收必须等于应收，不发生真实扣款");
    var header =
        new SaleWrite(
            numbers.next("XS"),
            request.requestKey(),
            hash,
            user.id(),
            user.realName(),
            quantity,
            total,
            request.paymentMethod(),
            request.receivedAmount(),
            now);
    if (mapper.create(header) != 1) throw new IllegalStateException("Sale insert failed");
    var categories =
        inventory.categories(products.values().stream().map(StockProduct::getCategoryId).toList());
    List<SaleItemWrite> lines = new ArrayList<>();
    int lineNo = 0;
    for (var entry : input.entrySet()) {
      var p = products.get(entry.getKey());
      String category = categories.get(p.getCategoryId());
      if (category == null) throw BusinessException.conflict("商品分类已不存在，请核查档案");
      lines.add(
          new SaleItemWrite(header.getId(), ++lineNo, entry.getValue().quantity(), p, category));
    }
    if (mapper.insertItems(lines) != lines.size())
      throw new IllegalStateException("Sale items insert failed");
    var items =
        mapper.items(header.getId()).stream()
            .collect(Collectors.toMap(SaleItem::productId, Function.identity()));
    var allocations =
        plan.stream()
            .map(
                t ->
                    new SaleAllocationWrite(
                        items.get(t.batch().productId()).id(),
                        t.batch().productId(),
                        t.batch().id(),
                        t.quantity()))
            .toList();
    if (mapper.insertAllocations(allocations) != allocations.size())
      throw new IllegalStateException("Sale allocations insert failed");
    var batchById =
        batches.stream().collect(Collectors.toMap(BatchInventory::id, Function.identity()));
    for (var allocation : mapper.allocations(header.getId()))
      inventory.deductSale(
          products.get(allocation.productId()),
          batchById.get(allocation.batchItemId()),
          allocation.quantity(),
          allocation.id(),
          header.getOrderNo(),
          user,
          now);
    audit.record(
        user, "SALE_COMPLETE", "SALE", header.getId().toString(), header.getOrderNo(), null);
    return queries.detail(header.getId(), user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public SaleDetailVO correct(long id, SaleCorrectionRequest request, CurrentUser user) {
    access.requireAdmin(user, "sale.order.correct");
    var order = mapper.lock(id, access.require(user, "sale.order.correct"));
    if (order == null) throw BusinessException.notFound();
    if (order.rowVersion() != request.expectedVersion())
      throw new BusinessException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "销售单已被处理，请刷新详情");
    if (order.status() != SaleStatus.COMPLETED)
      throw BusinessException.conflict("销售单已经纠错，不能再次恢复库存");
    if (!Boolean.TRUE.equals(request.goodsReturned())) throw SaleInput.bad("必须确认货物实际仍在或已完整收回");
    var allocations = mapper.allocations(id);
    if (allocations.isEmpty()) throw BusinessException.conflict("销售单缺少原始批次分配，不能纠错");
    var products =
        inventory.lockProducts(allocations.stream().map(SaleAllocation::productId).toList());
    var batches =
        inventory.lockBatchIds(allocations.stream().map(SaleAllocation::batchItemId).toList());
    var receipts = inventory.receipts(batches);
    var now = LocalDateTime.now(clock);
    if (!order.completedAt().toLocalDate().equals(now.toLocalDate())
        || order.completedAt().isAfter(now))
      throw BusinessException.conflict("仅允许当天整单纠错，跨日请核实后通过实际盘点处理");
    for (var b : batches)
      if (!receipts.containsKey(b.purchaseId())
          || !"IN_STOCK".equals(receipts.get(b.purchaseId()).status()))
        throw BusinessException.conflict("来源采购批次不再有效，不能恢复库存");
    var byId = batches.stream().collect(Collectors.toMap(BatchInventory::id, Function.identity()));
    for (var a : allocations)
      inventory.restoreSale(
          products.get(a.productId()),
          byId.get(a.batchItemId()),
          a.quantity(),
          a.id(),
          order.orderNo(),
          user,
          request.reason(),
          now);
    if (mapper.correct(id, user, now, request.reason()) != 1)
      throw BusinessException.conflict("销售单已被处理，请刷新");
    audit.record(
        user, "SALE_CORRECT", "SALE", Long.toString(id), order.orderNo(), request.reason());
    return queries.detail(id, user);
  }
}
