package com.supermarket.inventory.purchase.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.service.InventoryKernel;
import com.supermarket.inventory.purchase.entity.*;
import com.supermarket.inventory.purchase.enums.*;
import com.supermarket.inventory.purchase.mapper.PurchaseMapper;
import com.supermarket.inventory.purchase.vo.PurchaseDetailVO;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class PurchaseWorkflowService {
  private final PurchaseMapper mapper;
  private final PurchaseAccess access;
  private final InventoryKernel inventory;
  private final PurchaseQueryService queries;
  private final AuditService audit;
  private final Clock clock;

  public PurchaseWorkflowService(
      PurchaseMapper mapper,
      PurchaseAccess access,
      InventoryKernel inventory,
      PurchaseQueryService queries,
      AuditService audit,
      Clock clock) {
    this.mapper = mapper;
    this.access = access;
    this.inventory = inventory;
    this.queries = queries;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO submit(long id, CommandVersion request, CurrentUser user) {
    var order = access.lock(id, user, "purchase.order.submit");
    access.version(order, request.expectedVersion());
    if (order.status() != PurchaseStatus.DRAFT && order.status() != PurchaseStatus.REJECTED)
      throw BusinessException.conflict("仅草稿或已驳回采购可提交");
    validateAndReceive(order, user, false);
    if (mapper.submit(id, LocalDateTime.now(clock)) != 1) throw BusinessException.notFound();
    audit.record(user, "PURCHASE_SUBMIT", "PURCHASE", Long.toString(id), order.orderNo(), null);
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO withdraw(long id, CommandVersion request, CurrentUser user) {
    var order = access.lock(id, user, "purchase.order.withdraw");
    access.version(order, request.expectedVersion());
    pending(order);
    if (mapper.withdraw(id) != 1) throw BusinessException.notFound();
    audit.record(user, "PURCHASE_WITHDRAW", "PURCHASE", Long.toString(id), order.orderNo(), null);
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO approve(long id, CommandVersion request, CurrentUser user) {
    access.requireAdmin(user, "purchase.order.review");
    var order = access.lock(id, user, "purchase.order.review");
    access.version(order, request.expectedVersion());
    pending(order);
    LocalDateTime now = validateAndReceive(order, user, true);
    if (mapper.approve(id, user, now) != 1) throw BusinessException.notFound();
    audit.record(user, "PURCHASE_APPROVE", "PURCHASE", Long.toString(id), order.orderNo(), null);
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO reject(long id, ReasonCommand request, CurrentUser user) {
    access.requireAdmin(user, "purchase.order.reject");
    var order = access.lock(id, user, "purchase.order.reject");
    access.version(order, request.expectedVersion());
    pending(order);
    if (mapper.reject(id, user, LocalDateTime.now(clock), request.reason()) != 1)
      throw BusinessException.notFound();
    audit.record(
        user, "PURCHASE_REJECT", "PURCHASE", Long.toString(id), order.orderNo(), request.reason());
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO close(long id, ReasonCommand request, CurrentUser user) {
    var order = access.lock(id, user, "purchase.order.close");
    access.version(order, request.expectedVersion());
    if (order.status() != PurchaseStatus.DRAFT && order.status() != PurchaseStatus.REJECTED)
      throw BusinessException.conflict("仅草稿或已驳回采购可关闭，待审核单请先撤回");
    if (mapper.close(id, user, LocalDateTime.now(clock), request.reason()) != 1)
      throw BusinessException.notFound();
    audit.record(
        user, "PURCHASE_CLOSE", "PURCHASE", Long.toString(id), order.orderNo(), request.reason());
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO reverse(long id, ReasonCommand request, CurrentUser user) {
    access.requireAdmin(user, "purchase.order.reverse");
    var order = access.lock(id, user, "purchase.order.reverse");
    access.version(order, request.expectedVersion());
    if (order.status() != PurchaseStatus.IN_STOCK
        || order.reconcileStatus() != ReconcileStatus.UNRECONCILED)
      throw BusinessException.conflict("仅未核对的已入库单可撤销，请先撤回核对并确认没有后续业务");
    var items = mapper.items(id);
    var products = inventory.lockProducts(items.stream().map(PurchaseItem::productId).toList());
    var batches = inventory.lockBatches(id);
    LocalDateTime now = LocalDateTime.now(clock);
    inventory.reverse(products, batches, order.orderNo(), user, request.reason(), now);
    if (mapper.reverse(id, user, now, request.reason()) != 1) throw BusinessException.notFound();
    audit.record(
        user, "PURCHASE_REVERSE", "PURCHASE", Long.toString(id), order.orderNo(), request.reason());
    return queries.detail(id, user);
  }

  private LocalDateTime validateAndReceive(PurchaseOrder order, CurrentUser user, boolean receive) {
    LocalDateTime now = LocalDateTime.now(clock);
    if (order.arrivedAt() == null || order.arrivedAt().isAfter(now))
      throw BusinessException.conflict("请登记不晚于当前时间的实际到货时间");
    var supplier = mapper.lockSupplier(order.supplierId());
    if (supplier == null || !supplier.enabled())
      throw BusinessException.conflict("供应商已停用，请先修改或启用后再提交/审核");
    var items = mapper.items(order.id());
    var products = inventory.lockProducts(items.stream().map(PurchaseItem::productId).toList());
    var batches = inventory.lockBatches(order.id());
    inventory.validateReceipt(products, batches, now.toLocalDate());
    long qty = batches.stream().mapToLong(b -> b.quantity()).sum();
    BigDecimal amount =
        batches.stream()
            .map(b -> b.unitCost().multiply(BigDecimal.valueOf(b.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (qty != order.totalQty()
        || qty != order.totalReceivedQty()
        || amount.compareTo(order.totalAmount()) != 0)
      throw BusinessException.conflict("采购合计与明细不一致，请修正草稿后重提");
    if (receive) inventory.receive(products, batches, order.orderNo(), user, now);
    return now;
  }

  private void pending(PurchaseOrder order) {
    if (order.status() != PurchaseStatus.PENDING)
      throw BusinessException.conflict("该采购单已被处理或撤回，请刷新详情");
  }
}
