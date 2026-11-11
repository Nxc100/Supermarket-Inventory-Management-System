package com.supermarket.inventory.inventory.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.dto.InventoryQuery;
import com.supermarket.inventory.inventory.mapper.SaleableInventoryMapper;
import com.supermarket.inventory.inventory.vo.CashierInventoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleableInventoryService {
  private final SaleableInventoryMapper mapper;
  private final java.time.Clock clock;

  public SaleableInventoryService(SaleableInventoryMapper mapper, java.time.Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResult<CashierInventoryVO> page(InventoryQuery query) {
    if (query.getCategoryId() != null) Identifiers.parse(query.getCategoryId());
    return new PageResult<>(
        mapper.page(query, java.time.LocalDate.now(clock)),
        mapper.count(query),
        query.getPageNum(),
        query.getPageSize());
  }
}
