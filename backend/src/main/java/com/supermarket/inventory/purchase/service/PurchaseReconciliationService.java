package com.supermarket.inventory.purchase.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.purchase.dto.*;
import com.supermarket.inventory.purchase.entity.*;
import com.supermarket.inventory.purchase.enums.*;
import com.supermarket.inventory.purchase.mapper.*;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class PurchaseReconciliationService {
  private final PurchaseMapper orders;
  private final ReconciliationMapper mapper;
  private final PurchaseAccess access;
  private final PurchaseQueryService queries;
  private final AuditService audit;
  private final Clock clock;

  public PurchaseReconciliationService(
      PurchaseMapper orders,
      ReconciliationMapper mapper,
      PurchaseAccess access,
      PurchaseQueryService queries,
      AuditService audit,
      Clock clock) {
    this.orders = orders;
    this.mapper = mapper;
    this.access = access;
    this.queries = queries;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO save(
      long id, ReconcileSaveRequest request, CurrentUser user, boolean match) {
    var order = access.lock(id, user, "purchase.order.reconcile");
    version(order, request.expectedVersion(), request.expectedReconcileVersion());
    if (order.status() != PurchaseStatus.IN_STOCK
        || order.reconcileStatus() == ReconcileStatus.MATCHED)
      throw BusinessException.conflict("仅已入库且未确认一致的单据可核对，已核对单请先撤回核对");
    var items = orders.items(id);
    Map<Long, ReconcileSaveRequest.Line> input = new HashMap<>();
    for (var line : request.lines())
      if (input.put(Identifiers.parse(line.itemId()), line) != null) throw bad("核对明细存在重复行");
    if (input.size() != items.size()
        || !input.keySet().equals(items.stream().map(PurchaseItem::id).collect(Collectors.toSet())))
      throw bad("核对明细必须完整对应当前采购明细");
    BigDecimal subtotal = BigDecimal.ZERO;
    boolean same = true;
    for (var item : items) {
      var line = input.get(item.id());
      var externalAmount = line.externalUnitCost().multiply(BigDecimal.valueOf(line.externalQty()));
      if (externalAmount.compareTo(new BigDecimal("99999999999999.99")) > 0)
        throw bad("外部行金额超过允许范围");
      subtotal = subtotal.add(externalAmount);
      if (subtotal.compareTo(new BigDecimal("99999999999999.99")) > 0) throw bad("外部明细合计超过允许范围");
      same &=
          line.externalQty() == item.quantity()
              && line.externalUnitCost().compareTo(item.unitCost()) == 0;
    }
    same &=
        subtotal.compareTo(request.externalTotal()) == 0
            && request.externalTotal().compareTo(order.totalAmount()) == 0;
    if (match && !same) throw BusinessException.conflict("逐行数量、单价及外部合计尚不一致，请先登记差异");
    if (!match && same) throw bad("本次逐行及合计均一致，请使用确认一致");
    if (!match && (request.difference() == null || request.difference().trim().length() < 2))
      throw bad("存在差异时必须填写至少2字差异说明");
    if (match
        && order.reconcileStatus() == ReconcileStatus.DIFFERENCE
        && (request.resolution() == null || request.resolution().trim().length() < 2))
      throw bad("解决差异后确认一致必须填写处理说明");
    var before = snapshot(order, items);
    for (var item : items)
      if (mapper.line(id, item.id(), input.get(item.id())) != 1)
        throw BusinessException.conflict("核对明细已变化，请刷新");
    var status = match ? ReconcileStatus.MATCHED : ReconcileStatus.DIFFERENCE;
    if (mapper.save(id, status, request, user, LocalDateTime.now(clock)) != 1)
      throw BusinessException.notFound();
    var afterOrder = orders.find(id, access.require(user, "purchase.order.reconcile"));
    var after = snapshot(afterOrder, orders.items(id));
    audit.purchaseSnapshot(
        user,
        match ? "RECONCILE_MATCH" : "RECONCILE_DIFFERENCE",
        Long.toString(id),
        order.orderNo(),
        match ? request.resolution() : request.difference(),
        before,
        after);
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO withdraw(long id, ReconcileWithdrawRequest request, CurrentUser user) {
    var order = access.lock(id, user, "purchase.order.reconcile");
    version(order, request.expectedVersion(), request.expectedReconcileVersion());
    if (order.status() != PurchaseStatus.IN_STOCK
        || (order.reconcileStatus() != ReconcileStatus.MATCHED
            && order.reconcileStatus() != ReconcileStatus.DIFFERENCE))
      throw BusinessException.conflict("仅有差异或已核对的入库单可撤回核对");
    var items = orders.items(id);
    var before = snapshot(order, items);
    if (mapper.clearLines(id) != items.size() || mapper.withdraw(id) != 1)
      throw BusinessException.conflict("核对记录已变化，请刷新");
    var after = orders.find(id, access.require(user, "purchase.order.reconcile"));
    audit.purchaseSnapshot(
        user,
        "RECONCILE_WITHDRAW",
        Long.toString(id),
        order.orderNo(),
        request.reason(),
        before,
        snapshot(after, orders.items(id)));
    return queries.detail(id, user);
  }

  private void version(PurchaseOrder order, long rowVersion, int reconcileVersion) {
    access.version(order, rowVersion);
    if (order.reconcileVersion() != reconcileVersion)
      throw BusinessException.conflict("核对版本已更新，请刷新单据");
  }

  private ReconcileSnapshotVO snapshot(PurchaseOrder order, List<PurchaseItem> items) {
    return new ReconcileSnapshotVO(
        order.reconcileStatus(),
        order.externalTotalAmount(),
        order.reconcileReference(),
        order.reconcileDifference(),
        order.reconcileResolution(),
        items.stream()
            .map(
                i ->
                    new ReconcileSnapshotVO.Line(
                        i.id().toString(),
                        i.externalQty(),
                        i.externalUnitCost(),
                        i.reconcileNote()))
            .toList());
  }

  private BusinessException bad(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }
}
