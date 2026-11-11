package com.supermarket.inventory.statistics.controller;

import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.statistics.dto.*;
import com.supermarket.inventory.statistics.service.StatisticsService;
import com.supermarket.inventory.statistics.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {
  private final StatisticsService service;

  public StatisticsController(StatisticsService service) {
    this.service = service;
  }

  @GetMapping("/sales")
  @PreAuthorize("hasAuthority('statistics.sales')")
  @Operation(summary = "A12/C05 全店销售事件汇总、12月补零及商品聚合，不含他人单据与成本")
  public ApiResponse<SalesStatisticsVO> sales(
      @Valid @ModelAttribute SalesStatisticsQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.sales(query, user));
  }

  @GetMapping("/sales/slow-products")
  @PreAuthorize("hasAuthority('statistics.sales')")
  @Operation(summary = "A12/C05 当前可售且窗口零净销量滞销，首次入库不足窗口单列")
  public ApiResponse<SlowProductPageVO> slow(
      @Valid @ModelAttribute SalesStatisticsQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.slow(query, user));
  }

  @GetMapping("/inventory")
  @PreAuthorize("hasAuthority('statistics.inventory')")
  @Operation(summary = "A12/B05 当前账面库存分类占比、可售及批次预警数量")
  public ApiResponse<InventoryStatisticsVO> inventory(
      @Valid @ModelAttribute InventoryStatisticsQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.inventory(query, user));
  }
}
