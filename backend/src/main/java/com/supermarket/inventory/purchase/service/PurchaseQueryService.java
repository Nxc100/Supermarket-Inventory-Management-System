package com.supermarket.inventory.purchase.service;

import com.fasterxml.jackson.databind.*;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.purchase.dto.PurchaseQuery;
import com.supermarket.inventory.purchase.entity.*;
import com.supermarket.inventory.purchase.mapper.PurchaseMapper;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseQueryService {
  private final PurchaseMapper mapper;
  private final PurchaseAccess access;
  private final ObjectReader historyReader;

  public PurchaseQueryService(PurchaseMapper mapper, PurchaseAccess access, ObjectMapper json) {
    this.mapper = mapper;
    this.access = access;
    historyReader =
        json.readerFor(ReconcileSnapshotVO.class)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Transactional(readOnly = true)
  public PageResult<PurchaseSummaryVO> page(PurchaseQuery query, CurrentUser user) {
    var scope = access.require(user, "purchase.order");
    if (query.getSupplierId() != null) Identifiers.parse(query.getSupplierId());
    return new PageResult<>(
        mapper.page(query, scope).stream().map(PurchaseSummaryVO::from).toList(),
        mapper.count(query, scope),
        query.getPageNum(),
        query.getPageSize());
  }

  @Transactional(readOnly = true)
  public PurchaseDetailVO detail(long id, CurrentUser user) {
    var order = mapper.find(id, access.require(user, "purchase.order"));
    if (order == null) throw BusinessException.notFound();
    return PurchaseDetailVO.from(
        order,
        mapper.items(id).stream().map(PurchaseItemVO::from).toList(),
        mapper.reconcileHistory(id).stream().map(this::history).toList());
  }

  private ReconcileHistoryVO history(ReconcileHistoryRow row) {
    return new ReconcileHistoryVO(
        row.id().toString(),
        row.actorName(),
        row.action(),
        row.reason(),
        snapshot(row.beforeJson()),
        snapshot(row.afterJson()),
        row.occurredAt());
  }

  private ReconcileSnapshotVO snapshot(String json) {
    if (json == null) return null;
    try {
      return historyReader.readValue(json);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Invalid reconciliation history", e);
    }
  }
}
