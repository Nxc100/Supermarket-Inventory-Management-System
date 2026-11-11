package com.supermarket.inventory.ai.service;

import com.fasterxml.jackson.databind.*;
import com.supermarket.inventory.ai.entity.*;
import com.supermarket.inventory.ai.enums.SuggestionType;
import com.supermarket.inventory.ai.mapper.SuggestionMapper;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.statistics.dto.*;
import com.supermarket.inventory.statistics.mapper.StatisticsMapper;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class SuggestionSummaryBuilder {
  private final SuggestionMapper mapper;
  private final StatisticsMapper statistics;
  private final ObjectMapper json;
  private final Clock clock;

  public SuggestionSummaryBuilder(
      SuggestionMapper mapper, StatisticsMapper statistics, ObjectMapper json, Clock clock) {
    this.mapper = mapper;
    this.statistics = statistics;
    this.json = json;
    this.clock = clock;
  }

  /**
   * One read snapshot; the returned whitelist has no account, contact, cost or order-level fields.
   */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public JsonNode build(SuggestionScope scope) {
    var q = new SalesStatisticsQuery();
    q.setProductId(scope.productId());
    q.setCategoryId(scope.categoryId());
    q.setStartDate(scope.start());
    q.setEndDate(scope.end());
    q.setYear(scope.end().getYear());
    q.setPageSize(50);
    var sales = statistics.salesTotals(q);
    var today = LocalDate.now(clock);
    boolean hasSales = sales.completedCount() + sales.correctedCount() > 0;
    Map<String, Object> output = new LinkedHashMap<>();
    output.put("type", scope.type());
    output.put("periodStart", scope.start());
    output.put("periodEnd", scope.end());
    output.put("businessDate", today);
    output.put("salesHistory", hasSales ? "所选区间有销售事件" : "所选区间无销售历史");
    output.put(
        "interpretation",
        "数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。");
    if (scope.type() == SuggestionType.INVENTORY) {
      var inventory = new InventoryStatisticsQuery();
      inventory.setProductId(scope.productId());
      inventory.setCategoryId(scope.categoryId());
      var totals = statistics.inventoryTotals(inventory, today);
      if (!hasSales && !mapper.hasReceipt(q, today)) throw noData();
      var items = mapper.inventorySummary(q, today);
      long count = statistics.inventoryCount(inventory, today);
      output.put("currentInventory", totals);
      output.put("periodNetQty", sales.netQty());
      output.put("products", items);
      output.put(
          "coverage",
          Map.of(
              "matchingProducts",
              count,
              "includedProducts",
              items.size(),
              "truncated",
              count > items.size(),
              "selection",
              "最多50个商品，优先过期量、临期量，再按账面库存及商品ID；总计覆盖全部筛选范围。"));
    } else {
      if (!hasSales) throw noData();
      output.put("salesTotals", sales);
      var daily = statistics.daily(q);
      output.put("dailyTrend", daily.subList(Math.max(0, daily.size() - 366), daily.size()));
      output.put(
          "trendCoverage",
          Map.of(
              "eventDays",
              daily.size(),
              "includedDays",
              Math.min(366, daily.size()),
              "note",
              "仅含最近最多366个事件日，区间总计仍覆盖完整筛选。"));
      output.put(
          "hotProducts",
          statistics.products(q, 10, 0).stream().filter(p -> p.netQty() > 0).toList());
      output.put(
          "currentSlowWindow",
          Map.of(
              "start",
              today.minusDays(29),
              "end",
              today,
              "note",
              "当前有可售库存、窗口零净销量；首次有效入库不足30天单列观察不足，需结合缺货和到货人工判断。",
              "matchingProducts",
              statistics.slowCount(q, today, today.minusDays(29)),
              "products",
              statistics.slowProducts(q, today, today.minusDays(29))));
    }
    return json.valueToTree(output);
  }

  private BusinessException noData() {
    return new BusinessException(
        HttpStatus.BAD_REQUEST, "NO_BUSINESS_DATA", "当前范围没有可分析的实际业务数据，请先完成入库/销售或调整分析区间");
  }
}
