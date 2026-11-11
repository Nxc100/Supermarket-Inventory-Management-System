package com.supermarket.inventory.inventory.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.dto.*;
import com.supermarket.inventory.inventory.service.StockReadService;
import com.supermarket.inventory.inventory.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class StockReadController {
  private final StockReadService service;

  public StockReadController(StockReadService service) {
    this.service = service;
  }

  @GetMapping("/products")
  @PreAuthorize("hasAuthority('inventory.query')")
  @Operation(summary = "A08/B05 管理/采购库存，含账面/可售/过期与独立冻结状态")
  public ApiResponse<PageResult<StockInventoryVO>> products(
      @Valid @ModelAttribute StockQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.products(query, user));
  }

  @GetMapping("/products/{id}/batches")
  @PreAuthorize("hasAuthority('inventory.batch')")
  @Operation(summary = "A08 批次追溯，来源详情链接受采购归属限制")
  public ApiResponse<PageResult<StockBatchVO>> batches(
      @PathVariable long id,
      @Valid @ModelAttribute BatchQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.batches(id, query, user));
  }

  @GetMapping("/movements")
  @PreAuthorize("hasAuthority('inventory.movement')")
  @Operation(summary = "A08 库存流水，只读、按实际事件时间查询")
  public ApiResponse<PageResult<StockMovementVO>> movements(
      @Valid @ModelAttribute MovementQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.movements(query, user));
  }
}
