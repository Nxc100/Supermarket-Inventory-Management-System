package com.supermarket.inventory.purchase.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.config.CoreConfiguration;
import com.supermarket.inventory.inventory.entity.StockProduct;
import com.supermarket.inventory.inventory.service.InventoryKernel;
import com.supermarket.inventory.purchase.dto.*;
import com.supermarket.inventory.purchase.entity.*;
import com.supermarket.inventory.purchase.enums.PurchaseStatus;
import com.supermarket.inventory.purchase.mapper.PurchaseMapper;
import com.supermarket.inventory.purchase.vo.PurchaseDetailVO;
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
public class PurchaseDraftService {
  private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999.99");
  private final PurchaseMapper mapper;
  private final PurchaseAccess access;
  private final InventoryKernel inventory;
  private final PurchaseQueryService queries;
  private final AuditService audit;
  private final DocumentNumbers numbers;
  private final Clock clock;
  private final com.supermarket.inventory.security.BusinessActorGuard actors;

  public PurchaseDraftService(
      PurchaseMapper mapper,
      PurchaseAccess access,
      InventoryKernel inventory,
      PurchaseQueryService queries,
      AuditService audit,
      DocumentNumbers numbers,
      Clock clock,
      com.supermarket.inventory.security.BusinessActorGuard actors) {
    this.mapper = mapper;
    this.access = access;
    this.inventory = inventory;
    this.queries = queries;
    this.audit = audit;
    this.numbers = numbers;
    this.clock = clock;
    this.actors = actors;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO create(PurchaseSaveRequest request, CurrentUser user) {
    access.require(user, "purchase.order.create");
    actors.newTask(user);
    if (request.expectedVersion() != null) throw bad("新建采购不能指定既有版本");
    var prepared = prepare(request, null, user);
    var header = prepared.header();
    if (mapper.create(header) != 1) throw new IllegalStateException("Purchase insert failed");
    writeItems(header.getId(), prepared.items(), header.getNow());
    audit.record(
        user, "PURCHASE_CREATE", "PURCHASE", header.getId().toString(), header.getOrderNo(), null);
    return queries.detail(header.getId(), user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PurchaseDetailVO update(long id, PurchaseSaveRequest request, CurrentUser user) {
    var old = access.lock(id, user, "purchase.order.edit");
    access.version(old, request.expectedVersion());
    if (old.status() != PurchaseStatus.DRAFT && old.status() != PurchaseStatus.REJECTED)
      throw BusinessException.conflict("仅草稿或已驳回采购可修改，请先撤回待审核单据");
    var prepared = prepare(request, old, user);
    var header = prepared.header();
    header.setId(id);
    if (mapper.update(header) != 1) throw BusinessException.notFound();
    var count = mapper.items(id).size();
    if (mapper.deleteItems(id) != count) throw BusinessException.conflict("采购明细已变化，请刷新");
    writeItems(id, prepared.items(), header.getNow());
    audit.record(user, "PURCHASE_EDIT", "PURCHASE", Long.toString(id), old.orderNo(), null);
    return queries.detail(id, user);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void delete(long id, long expected, CurrentUser user) {
    var old = access.lock(id, user, "purchase.order.delete_draft");
    access.version(old, expected);
    if (old.status() != PurchaseStatus.DRAFT || old.firstSubmittedAt() != null)
      throw BusinessException.conflict("仅从未提交过的草稿可删除，其他单据请关闭并保留轨迹");
    var items = mapper.items(id);
    inventory.lockProducts(items.stream().map(PurchaseItem::productId).toList());
    if (mapper.deleteItems(id) != items.size() || mapper.delete(id) != 1)
      throw BusinessException.conflict("采购记录已变化，请刷新");
    audit.record(user, "PURCHASE_DELETE_DRAFT", "PURCHASE", Long.toString(id), old.orderNo(), null);
  }

  private record Prepared(PurchaseWrite header, List<PurchaseItem> items) {}

  private Prepared prepare(PurchaseSaveRequest request, PurchaseOrder old, CurrentUser user) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime arrived =
        request.arrivedAt() == null
            ? null
            : request.arrivedAt().atZoneSameInstant(CoreConfiguration.ZONE).toLocalDateTime();
    if (arrived != null && arrived.isAfter(now)) throw bad("到货时间不能晚于当前时间");
    long supplierId = Identifiers.parse(request.supplierId());
    var supplier = mapper.lockSupplier(supplierId);
    if (supplier == null) throw BusinessException.notFound();
    if (!supplier.enabled()) throw BusinessException.conflict("供应商已停用，请选择已启用供应商");
    List<Long> ids = new ArrayList<>();
    request.items().forEach(i -> ids.add(Identifiers.parse(i.productId())));
    if (old != null) mapper.items(old.id()).forEach(i -> ids.add(i.productId()));
    var products = inventory.lockProducts(ids);
    List<Long> categoryIds =
        products.values().stream().map(StockProduct::getCategoryId).distinct().toList();
    Map<Long, String> categories =
        mapper.categoryNames(categoryIds).stream()
            .collect(Collectors.toMap(CategoryName::id, CategoryName::name));
    List<PurchaseItem> items = new ArrayList<>();
    long total = 0, received = 0;
    BigDecimal amount = BigDecimal.ZERO.setScale(2);
    for (int n = 0; n < request.items().size(); n++) {
      var line = request.items().get(n);
      var product = products.get(Identifiers.parse(line.productId()));
      if (!product.getEnabled() || !product.getCanPurchase())
        throw BusinessException.conflict("第" + (n + 1) + "行商品已停用或禁止采购");
      InventoryKernel.validateDates(
          product.getManagesExpiry(),
          line.productionDate(),
          line.expiryDate(),
          now.toLocalDate(),
          false,
          "第" + (n + 1) + "行");
      BigDecimal rowAmount = line.unitCost().multiply(BigDecimal.valueOf(line.quantity()));
      if (rowAmount.compareTo(MAX_AMOUNT) > 0) throw bad("第" + (n + 1) + "行金额超过允许范围");
      total = Math.addExact(total, line.quantity());
      received = Math.addExact(received, line.receivedQty() == null ? 0 : line.receivedQty());
      amount = amount.add(rowAmount);
      items.add(
          new PurchaseItem(
              null,
              null,
              n + 1,
              product.getId(),
              product.getCode(),
              product.getBarcode(),
              product.getName(),
              product.getCategoryId(),
              categories.get(product.getCategoryId()),
              product.getSpecification(),
              product.getUnit(),
              product.getManagesExpiry(),
              line.quantity(),
              line.receivedQty(),
              line.unitCost(),
              rowAmount,
              line.supplierBatchNo(),
              line.productionDate(),
              line.expiryDate(),
              null,
              null,
              null,
              null,
              null,
              null));
    }
    if (total > InventoryKernel.MAX_STOCK || amount.compareTo(MAX_AMOUNT) > 0)
      throw bad("采购数量或总金额超过允许范围");
    String number = old == null ? numbers.next("CG") : old.orderNo();
    PurchaseWrite header =
        new PurchaseWrite(
            number,
            supplierId,
            supplier.name(),
            supplier.contactName(),
            supplier.contactPhone(),
            supplier.address(),
            old == null ? user.id() : old.operatorId(),
            old == null ? user.realName() : old.operatorName(),
            total,
            received,
            amount,
            request.expectedArrivalDate(),
            arrived,
            request.remark(),
            now);
    return new Prepared(header, List.copyOf(items));
  }

  private void writeItems(long id, List<PurchaseItem> items, LocalDateTime now) {
    if (mapper.insertItems(id, items, now) != items.size())
      throw new IllegalStateException("Purchase items insert failed");
  }

  private BusinessException bad(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }
}
