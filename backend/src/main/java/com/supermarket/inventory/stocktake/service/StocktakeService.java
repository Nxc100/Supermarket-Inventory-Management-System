package com.supermarket.inventory.stocktake.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.entity.*;
import com.supermarket.inventory.inventory.service.InventoryKernel;
import com.supermarket.inventory.security.*;
import com.supermarket.inventory.stocktake.dto.*;
import com.supermarket.inventory.stocktake.entity.*;
import com.supermarket.inventory.stocktake.enums.StocktakeStatus;
import com.supermarket.inventory.stocktake.mapper.StocktakeMapper;
import com.supermarket.inventory.stocktake.vo.*;
import com.supermarket.inventory.system.service.AuditService;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Task state owns its snapshot; only InventoryKernel owns freezes, balances and ledger writes. */
@Service
@Transactional(readOnly = true)
public class StocktakeService {
  private final StocktakeMapper mapper;
  private final InventoryKernel inventory;
  private final AuditService audit;
  private final DocumentNumbers numbers;
  private final Clock clock;
  private final BusinessActorGuard actors;

  public StocktakeService(
      StocktakeMapper mapper,
      InventoryKernel inventory,
      AuditService audit,
      DocumentNumbers numbers,
      Clock clock,
      BusinessActorGuard actors) {
    this.mapper = mapper;
    this.inventory = inventory;
    this.audit = audit;
    this.numbers = numbers;
    this.clock = clock;
    this.actors = actors;
  }

  private BusinessScope require(CurrentUser user, String action) {
    if (!Set.of("ADMIN", "PURCHASER").contains(user.roleCode())
        || !user.permissions().contains(action))
      throw new AccessDeniedException("Stocktake access denied");
    return BusinessScope.of(user);
  }

  public PageResult<StocktakeSummaryVO> page(StocktakeQuery q, CurrentUser user) {
    var scope = require(user, "inventory.stocktake");
    return new PageResult<>(
        mapper.page(q, scope), mapper.count(q, scope), q.getPageNum(), q.getPageSize());
  }

  public StocktakeDetailVO detail(long id, CurrentUser user) {
    var order = mapper.find(id, require(user, "inventory.stocktake"));
    if (order == null) throw BusinessException.notFound();
    return new StocktakeDetailVO(
        mapper.summary(id),
        mapper.itemViews(id),
        order.remark(),
        order.reviewerName(),
        order.reviewedAt(),
        order.rejectionReason(),
        order.closedAt(),
        order.closeReason(),
        !Set.of(StocktakeStatus.COMPLETED, StocktakeStatus.CLOSED).contains(order.status()));
  }

  private StocktakeOrder lock(
      long id, long version, CurrentUser user, String action, StocktakeStatus... allowed) {
    var order = mapper.lock(id, require(user, "inventory.stocktake." + action));
    if (order == null) throw BusinessException.notFound();
    if (order.rowVersion() != version)
      throw new BusinessException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "盘点已被修改或处理，请刷新详情后重试");
    if (!Arrays.asList(allowed).contains(order.status()))
      throw BusinessException.conflict("盘点当前状态不允许此操作，请刷新详情");
    return order;
  }

  private void admin(CurrentUser user) {
    if (!"ADMIN".equals(user.roleCode()))
      throw new AccessDeniedException("Stocktake review requires administrator");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO start(StocktakeStartRequest request, CurrentUser user) {
    require(user, "inventory.stocktake.start");
    actors.newTask(user);
    var ids = request.productIds().stream().map(Identifiers::parse).sorted().toList();
    if (new HashSet<>(ids).size() != ids.size()) throw bad("盘点商品不能重复");
    var products = inventory.lockProducts(ids);
    for (var p : products.values()) inventory.unfrozen(p);
    var batches = activeBatches(inventory.lockStocktakeBatches(ids));
    if (batches.isEmpty()) throw BusinessException.conflict("所选商品没有有效入库批次，请先通过采购建立真实来源");
    if (batches.size() > 2000) throw bad("本次批次超过2000，请缩小商品范围分次盘点");
    for (var p : products.values()) {
      var own = batches.stream().filter(b -> b.productId().equals(p.getId())).toList();
      if (own.isEmpty()) throw BusinessException.conflict("商品「" + p.getName() + "」从未有效入库，请先建立真实来源");
      if (own.stream().mapToLong(BatchInventory::stockQty).sum() != p.getStockQty())
        throw BusinessException.conflict("商品「" + p.getName() + "」账面与批次不一致，请管理员核查");
    }
    var header =
        new StocktakeWrite(
            numbers.next("PD"),
            user.id(),
            user.realName(),
            request.remark(),
            LocalDateTime.now(clock));
    if (mapper.create(header) != 1) throw new IllegalStateException("Stocktake insert failed");
    inventory.freeze(products, header.getId());
    if (mapper.insertItems(header.getId(), batches) != batches.size())
      throw new IllegalStateException("Stocktake snapshot failed");
    audit.record(
        user, "STOCKTAKE_START", "STOCKTAKE", header.getId().toString(), header.getOrderNo(), null);
    return detail(header.getId(), user);
  }

  private List<BatchInventory> activeBatches(List<BatchInventory> batches) {
    var receipts = inventory.receipts(batches);
    return batches.stream()
        .filter(
            b ->
                receipts.containsKey(b.purchaseId())
                    && "IN_STOCK".equals(receipts.get(b.purchaseId()).status()))
        .toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO save(long id, StocktakeSaveRequest request, CurrentUser user) {
    var order =
        lock(
            id,
            request.expectedVersion(),
            user,
            "save",
            StocktakeStatus.COUNTING,
            StocktakeStatus.REJECTED);
    var existing =
        mapper.items(id).stream().collect(Collectors.toMap(StocktakeItem::id, Function.identity()));
    var seen = new HashSet<Long>();
    for (var line : request.items()) {
      long itemId = Identifiers.parse(line.id());
      var item = existing.get(itemId);
      if (item == null || !seen.add(itemId)) throw bad("盘点明细重复或不属于本单，请刷新");
      validateLine(line.actualQty(), item.bookQty(), line.reasonType(), line.reasonNote());
    }
    if (seen.size() != existing.size()) throw bad("请提交本盘点全部批次，未清点行可保留空白");
    for (var line : request.items())
      if (mapper.saveLine(id, line) != 1)
        throw new IllegalStateException("Stocktake line update failed");
    if (mapper.touch(id) != 1) throw new IllegalStateException("Stocktake version update failed");
    audit.record(user, "STOCKTAKE_SAVE", "STOCKTAKE", Long.toString(id), order.orderNo(), null);
    return detail(id, user);
  }

  private void validateLine(Long actual, long book, Object reason, String note) {
    if (actual != null && (actual < 0 || actual > InventoryKernel.MAX_STOCK))
      throw bad("实盘数量必须为范围内非负整数");
    if (actual != null && actual != book && (reason == null || note == null || note.length() < 2))
      throw bad("差异行必须选择原因并填写至少2字说明");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO submit(long id, CommandVersion request, CurrentUser user) {
    var order =
        lock(
            id,
            request.expectedVersion(),
            user,
            "submit",
            StocktakeStatus.COUNTING,
            StocktakeStatus.REJECTED);
    var items = mapper.items(id);
    complete(items);
    var products = inventory.lockProducts(items.stream().map(StocktakeItem::productId).toList());
    for (var p : products.values()) inventory.ownFreeze(p, id);
    transition(order, StocktakeStatus.PENDING, user, null);
    return detail(id, user);
  }

  private void complete(List<StocktakeItem> items) {
    if (items.isEmpty()) throw BusinessException.conflict("盘点缺少批次明细，请关闭重建");
    for (var item : items) {
      if (item.actualQty() == null) throw BusinessException.conflict("仍有未填写实盘数的批次；0是有效数，空白不能提交");
      validateLine(item.actualQty(), item.bookQty(), item.reasonType(), item.reasonNote());
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO withdraw(long id, CommandVersion request, CurrentUser user) {
    var order = lock(id, request.expectedVersion(), user, "withdraw", StocktakeStatus.PENDING);
    transition(order, StocktakeStatus.COUNTING, user, null);
    return detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO reject(long id, ReasonCommand request, CurrentUser user) {
    admin(user);
    var order = lock(id, request.expectedVersion(), user, "reject", StocktakeStatus.PENDING);
    transition(order, StocktakeStatus.REJECTED, user, request.reason());
    return detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO approve(long id, CommandVersion request, CurrentUser user) {
    admin(user);
    var order = lock(id, request.expectedVersion(), user, "review", StocktakeStatus.PENDING);
    var items = mapper.items(id);
    complete(items);
    var products = inventory.lockProducts(items.stream().map(StocktakeItem::productId).toList());
    for (var p : products.values()) inventory.ownFreeze(p, id);
    var batches = activeBatches(inventory.lockStocktakeBatches(products.keySet()));
    var byId = batches.stream().collect(Collectors.toMap(BatchInventory::id, Function.identity()));
    if (batches.size() != items.size()) throw BusinessException.conflict("来源批次范围已变化，请关闭重建盘点");
    for (var item : items) {
      var b = byId.get(item.batchItemId());
      if (b == null || !b.productId().equals(item.productId()) || b.stockQty() != item.bookQty())
        throw BusinessException.conflict("批次库存与盘点快照不一致，请关闭重建，不能按旧数调整");
    }
    for (var p : products.values())
      if (batches.stream()
              .filter(b -> b.productId().equals(p.getId()))
              .mapToLong(BatchInventory::stockQty)
              .sum()
          != p.getStockQty()) throw BusinessException.conflict("商品账面与批次合计不一致，请关闭后核查");
    var now = LocalDateTime.now(clock);
    for (var item : items)
      inventory.adjustStocktake(
          products.get(item.productId()),
          byId.get(item.batchItemId()),
          item.actualQty(),
          item.id(),
          id,
          order.orderNo(),
          user,
          item.reasonNote(),
          now);
    inventory.release(products, id);
    transition(order, StocktakeStatus.COMPLETED, user, null, now);
    return detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StocktakeDetailVO close(long id, ReasonCommand request, CurrentUser user) {
    var order =
        lock(
            id,
            request.expectedVersion(),
            user,
            "close",
            StocktakeStatus.COUNTING,
            StocktakeStatus.REJECTED,
            StocktakeStatus.PENDING);
    if (!"ADMIN".equals(user.roleCode()) && order.status() == StocktakeStatus.PENDING)
      throw BusinessException.conflict("待审核盘点请先撤回，再关闭");
    var productIds = new HashSet<>(inventory.frozenProductIds(id));
    mapper.items(id).forEach(item -> productIds.add(item.productId()));
    var products =
        productIds.isEmpty()
            ? new LinkedHashMap<Long, StockProduct>()
            : inventory.lockProducts(productIds);
    // An abnormal task may have lost its pointer. Close it without touching a different task's
    // freeze.
    products.values().removeIf(p -> !Long.valueOf(id).equals(p.getFrozenStocktakeId()));
    inventory.release(products, id);
    transition(order, StocktakeStatus.CLOSED, user, request.reason());
    return detail(id, user);
  }

  private void transition(
      StocktakeOrder order, StocktakeStatus status, CurrentUser user, String reason) {
    transition(order, status, user, reason, LocalDateTime.now(clock));
  }

  private void transition(
      StocktakeOrder order,
      StocktakeStatus status,
      CurrentUser user,
      String reason,
      LocalDateTime occurredAt) {
    if (mapper.transition(order.id(), status, user, reason, occurredAt) != 1)
      throw new IllegalStateException("Stocktake transition failed");
    audit.record(
        user,
        "STOCKTAKE_" + status.name(),
        "STOCKTAKE",
        order.id().toString(),
        order.orderNo(),
        reason);
  }

  private BusinessException bad(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }
}
