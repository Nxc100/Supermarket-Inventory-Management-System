package com.supermarket.inventory.statistics.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.statistics.dto.*;
import com.supermarket.inventory.statistics.mapper.StatisticsMapper;
import com.supermarket.inventory.statistics.vo.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Read-only reports use a consistent snapshot and never hold inventory write locks. */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StatisticsService {
  private final StatisticsMapper mapper;
  private final Clock clock;

  public StatisticsService(StatisticsMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  private void require(CurrentUser user, String type, String role) {
    if (!Set.of("ADMIN", role).contains(user.roleCode())
        || !user.permissions().contains("statistics." + type))
      throw new AccessDeniedException("Statistics scope denied");
  }

  private void salesQuery(SalesStatisticsQuery q) {
    if (q.getProductId() != null) Identifiers.parse(q.getProductId());
    if (q.getCategoryId() != null) Identifiers.parse(q.getCategoryId());
    var today = LocalDate.now(clock);
    if (q.getYear() == null) q.setYear(today.getYear());
    if (q.getEndDate() == null) q.setEndDate(today);
    if (q.getStartDate() == null) q.setStartDate(q.getEndDate().minusDays(29));
    if (!q.isDateRangeValid() || q.getEndDate().getYear() > 9998 || q.getStartDate().getYear() < 1)
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请选择有效日期区间，结束日期不得早于开始日期");
  }

  public SalesStatisticsVO sales(SalesStatisticsQuery q, CurrentUser user) {
    require(user, "sales", "CASHIER");
    salesQuery(q);
    var months =
        mapper.monthly(q, LocalDate.of(q.getYear(), 1, 1)).stream()
            .collect(Collectors.toMap(SalesStatisticsVO.MonthRow::month, Function.identity()));
    var full = new ArrayList<SalesStatisticsVO.MonthRow>();
    for (int month = 1; month <= 12; month++)
      full.add(
          months.getOrDefault(
              month, new SalesStatisticsVO.MonthRow(month, 0, BigDecimal.ZERO.setScale(2))));
    return new SalesStatisticsVO(
        mapper.products(q, q.getPageSize(), q.getOffset()),
        mapper.productCount(q),
        q.getPageNum(),
        q.getPageSize(),
        q.getYear(),
        q.getStartDate(),
        q.getEndDate(),
        LocalDateTime.now(clock),
        mapper.salesTotals(q),
        full,
        mapper.daily(q),
        mapper.products(q, 10, 0).stream().filter(p -> p.netQty() > 0).toList());
  }

  public SlowProductPageVO slow(SalesStatisticsQuery q, CurrentUser user) {
    require(user, "sales", "CASHIER");
    salesQuery(q);
    var today = LocalDate.now(clock);
    var start = today.minusDays(q.getWindowDays() - 1L);
    return new SlowProductPageVO(
        mapper.slowProducts(q, today, start),
        mapper.slowCount(q, today, start),
        q.getPageNum(),
        q.getPageSize(),
        start,
        today,
        q.getWindowDays(),
        LocalDateTime.now(clock));
  }

  public InventoryStatisticsVO inventory(InventoryStatisticsQuery q, CurrentUser user) {
    require(user, "inventory", "PURCHASER");
    if (q.getProductId() != null) Identifiers.parse(q.getProductId());
    if (q.getCategoryId() != null) Identifiers.parse(q.getCategoryId());
    var now = LocalDateTime.now(clock);
    var today = now.toLocalDate();
    var totals = mapper.inventoryTotals(q, today);
    var categories =
        mapper.categories(q, today).stream()
            .map(
                c ->
                    new InventoryStatisticsVO.CategoryRow(
                        c.categoryId(),
                        c.categoryName(),
                        c.bookQty(),
                        totals.bookQty() == 0
                            ? BigDecimal.ZERO.setScale(2)
                            : BigDecimal.valueOf(c.bookQty())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(
                                    BigDecimal.valueOf(totals.bookQty()), 2, RoundingMode.HALF_UP)))
            .toList();
    return new InventoryStatisticsVO(
        mapper.inventoryProducts(q, today),
        mapper.inventoryCount(q, today),
        q.getPageNum(),
        q.getPageSize(),
        today,
        now,
        totals,
        categories,
        new InventoryStatisticsVO.Filters(
            q.getKeyword(), q.getCategoryId(), q.getProductId(), q.getEnabled()));
  }
}
