package com.supermarket.inventory.sale.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.sale.dto.SaleQuery;
import com.supermarket.inventory.sale.enums.*;
import com.supermarket.inventory.sale.mapper.SaleMapper;
import com.supermarket.inventory.sale.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import java.time.*;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SaleQueryService {
  private final SaleMapper mapper;
  private final SaleAccess access;
  private final Clock clock;

  public SaleQueryService(SaleMapper mapper, SaleAccess access, Clock clock) {
    this.mapper = mapper;
    this.access = access;
    this.clock = clock;
  }

  public SalePageVO page(SaleQuery query, CurrentUser user) {
    var scope = access.require(user, "sale.order");
    if (query.getProductId() != null) Identifiers.parse(query.getProductId());
    if (query.getCashierId() != null) {
      long cashier = Identifiers.parse(query.getCashierId());
      if (!scope.admin() && cashier != user.id())
        throw new AccessDeniedException("Cashier filter outside own scope");
    }
    if (query.getEndDate() == null) query.setEndDate(LocalDate.now(clock));
    if (query.getStartDate() == null) query.setStartDate(query.getEndDate());
    if (!query.isDateRangeValid()) throw SaleInput.bad("结束日期不能早于开始日期");
    return new SalePageVO(
        mapper.page(query, scope),
        mapper.count(query, scope),
        query.getPageNum(),
        query.getPageSize(),
        mapper.totals(query, scope));
  }

  public SaleDetailVO detail(long id, CurrentUser user) {
    var order = mapper.find(id, access.require(user, "sale.order"));
    if (order == null) throw BusinessException.notFound();
    var items = mapper.items(id).stream().map(SaleItemVO::from).toList();
    boolean canCorrect =
        "ADMIN".equals(user.roleCode())
            && user.permissions().contains("sale.order.correct")
            && order.status() == SaleStatus.COMPLETED
            && order.completedAt().toLocalDate().equals(LocalDate.now(clock));
    return new SaleDetailVO(
        SaleSummaryVO.from(order, items.size()),
        items,
        new SaleDetailVO.Correction(
            order.correctedBy() == null ? null : order.correctedBy().toString(),
            order.correctorName(),
            order.correctedAt(),
            order.correctionReason()),
        canCorrect,
        order.status() == SaleStatus.CORRECTED
            && mapper.expiredSourceStock(id, LocalDate.now(clock)));
  }

  public SaleDetailVO byRequest(String key, CurrentUser user) {
    access.require(user, "sale.order");
    if (!key.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
      throw SaleInput.bad("提交标识格式不合法");
    var order = mapper.byKey(key);
    if (order == null || order.cashierId() != user.id()) throw BusinessException.notFound();
    return detail(order.id(), user);
  }

  public List<CashierOptionVO> cashiers(CurrentUser user) {
    access.requireAdmin(user, "sale.order");
    return mapper.cashiers();
  }

  public SaleContextVO context(CurrentUser user) {
    access.require(user, "sale.order");
    String configured = mapper.defaultPayment();
    PaymentMethod payment;
    try {
      payment = configured == null ? PaymentMethod.CASH : PaymentMethod.valueOf(configured);
    } catch (IllegalArgumentException e) {
      payment = PaymentMethod.CASH;
    }
    return new SaleContextVO(LocalDate.now(clock), payment);
  }
}
