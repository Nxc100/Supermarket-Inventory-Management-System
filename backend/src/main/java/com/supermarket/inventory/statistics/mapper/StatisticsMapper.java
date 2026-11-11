package com.supermarket.inventory.statistics.mapper;

import com.supermarket.inventory.inventory.vo.StockInventoryVO;
import com.supermarket.inventory.statistics.dto.*;
import com.supermarket.inventory.statistics.vo.*;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StatisticsMapper {
  SalesStatisticsVO.Totals salesTotals(@Param("q") SalesStatisticsQuery q);

  List<SalesStatisticsVO.MonthRow> monthly(
      @Param("q") SalesStatisticsQuery q, @Param("yearStart") LocalDate yearStart);

  List<SalesStatisticsVO.DayRow> daily(@Param("q") SalesStatisticsQuery q);

  List<SalesStatisticsVO.ProductRow> products(
      @Param("q") SalesStatisticsQuery q, @Param("limit") int limit, @Param("offset") long offset);

  long productCount(@Param("q") SalesStatisticsQuery q);

  List<SlowProductPageVO.Row> slowProducts(
      @Param("q") SalesStatisticsQuery q,
      @Param("today") LocalDate today,
      @Param("windowStart") LocalDate windowStart);

  long slowCount(
      @Param("q") SalesStatisticsQuery q,
      @Param("today") LocalDate today,
      @Param("windowStart") LocalDate windowStart);

  InventoryStatisticsVO.Totals inventoryTotals(
      @Param("query") InventoryStatisticsQuery query, @Param("today") LocalDate today);

  List<InventoryStatisticsVO.CategoryRow> categories(
      @Param("query") InventoryStatisticsQuery query, @Param("today") LocalDate today);

  List<StockInventoryVO> inventoryProducts(
      @Param("query") InventoryStatisticsQuery query, @Param("today") LocalDate today);

  long inventoryCount(
      @Param("query") InventoryStatisticsQuery query, @Param("today") LocalDate today);
}
