package com.supermarket.inventory.purchase.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.purchase.dto.SupplierSummaryQuery;
import com.supermarket.inventory.purchase.mapper.PurchaseLookupMapper;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseLookupService {
  private final PurchaseLookupMapper mapper;
  private final PurchaseAccess access;
  private final Clock clock;

  public PurchaseLookupService(PurchaseLookupMapper mapper, PurchaseAccess access, Clock clock) {
    this.mapper = mapper;
    this.access = access;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResult<SupplierOptionVO> suppliers(PageQuery query, CurrentUser user) {
    access.require(user, "purchase.order");
    return new PageResult<>(
        mapper.suppliers(query),
        mapper.supplierCount(query),
        query.getPageNum(),
        query.getPageSize());
  }

  @Transactional(readOnly = true)
  public PageResult<SupplierPurchaseSummaryVO> summary(
      SupplierSummaryQuery query, CurrentUser user) {
    var scope = access.require(user, "purchase.order.reconcile");
    if (query.getSupplierId() != null) Identifiers.parse(query.getSupplierId());
    if (query.getEndDate() == null) query.setEndDate(LocalDate.now(clock));
    if (query.getStartDate() == null) query.setStartDate(query.getEndDate().minusDays(29));
    if (query.getEndDate().isBefore(query.getStartDate()))
      throw new BusinessException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "结束日期不能早于开始日期");
    return new PageResult<>(
        mapper.summary(query, scope),
        mapper.summaryCount(query, scope),
        query.getPageNum(),
        query.getPageSize());
  }

  public PurchaseContextVO context(CurrentUser user) {
    access.require(user, "purchase.order");
    LocalDateTime now = LocalDateTime.now(clock);
    return new PurchaseContextVO(
        now.toLocalDate(), now, now.toLocalDate().minusDays(29), now.toLocalDate());
  }
}
