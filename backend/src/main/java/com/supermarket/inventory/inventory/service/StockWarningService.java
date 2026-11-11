package com.supermarket.inventory.inventory.service;

import com.supermarket.inventory.common.Identifiers;
import com.supermarket.inventory.inventory.dto.WarningQuery;
import com.supermarket.inventory.inventory.mapper.StockWarningMapper;
import com.supermarket.inventory.inventory.vo.WarningPageVO;
import com.supermarket.inventory.security.CurrentUser;
import java.time.*;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockWarningService {
  private final StockWarningMapper mapper;
  private final Clock clock;

  public StockWarningService(StockWarningMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public WarningPageVO page(WarningQuery query, CurrentUser user) {
    if (!Set.of("ADMIN", "PURCHASER").contains(user.roleCode())
        || !user.permissions().contains("inventory.warning"))
      throw new AccessDeniedException("Stock warnings denied");
    if (query.getCategoryId() != null) Identifiers.parse(query.getCategoryId());
    if (query.getProductId() != null) Identifiers.parse(query.getProductId());
    var now = LocalDateTime.now(clock);
    var today = now.toLocalDate();
    return new WarningPageVO(
        mapper.page(query, today),
        mapper.count(query, today),
        query.getPageNum(),
        query.getPageSize(),
        query.getType(),
        today,
        now);
  }
}
