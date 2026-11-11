package com.supermarket.inventory.inventory.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.dto.*;
import com.supermarket.inventory.inventory.mapper.StockReadMapper;
import com.supermarket.inventory.inventory.vo.*;
import com.supermarket.inventory.security.*;
import java.time.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockReadService {
  private final StockReadMapper mapper;
  private final Clock clock;

  public StockReadService(StockReadMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  private BusinessScope require(CurrentUser user, String permission) {
    if (!user.permissions().contains(permission)
        || !java.util.Set.of("ADMIN", "PURCHASER").contains(user.roleCode()))
      throw new AccessDeniedException("Inventory operator access denied");
    return BusinessScope.of(user);
  }

  public PageResult<StockInventoryVO> products(StockQuery query, CurrentUser user) {
    require(user, "inventory.query");
    if (query.getCategoryId() != null) Identifiers.parse(query.getCategoryId());
    LocalDate today = LocalDate.now(clock);
    return new PageResult<>(
        mapper.products(query, today),
        mapper.productCount(query, today),
        query.getPageNum(),
        query.getPageSize());
  }

  public PageResult<StockBatchVO> batches(long id, BatchQuery query, CurrentUser user) {
    var scope = require(user, "inventory.batch");
    if (!mapper.exists(id)) throw BusinessException.notFound();
    return new PageResult<>(
        mapper.batches(
            id, query, LocalDate.now(clock), scope, user.permissions().contains("purchase.order")),
        mapper.batchCount(id, query),
        query.getPageNum(),
        query.getPageSize());
  }

  public PageResult<StockMovementVO> movements(MovementQuery query, CurrentUser user) {
    var scope = require(user, "inventory.movement");
    if (query.getProductId() != null) Identifiers.parse(query.getProductId());
    if (query.getBatchId() != null) Identifiers.parse(query.getBatchId());
    return new PageResult<>(
        mapper.movements(query, scope, user.permissions().contains("purchase.order")),
        mapper.movementCount(query),
        query.getPageNum(),
        query.getPageSize());
  }
}
