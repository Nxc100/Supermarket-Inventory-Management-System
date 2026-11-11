package com.supermarket.inventory.sale.controller;

import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.sale.dto.*;
import com.supermarket.inventory.sale.service.*;
import com.supermarket.inventory.sale.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {
  private final SaleQueryService queries;
  private final SaleQuoteService quotes;
  private final SaleCheckoutService checkout;
  private final SaleCommandService commands;

  public SaleController(
      SaleQueryService queries,
      SaleQuoteService quotes,
      SaleCheckoutService checkout,
      SaleCommandService commands) {
    this.queries = queries;
    this.quotes = quotes;
    this.checkout = checkout;
    this.commands = commands;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('sale.order')")
  @Operation(summary = "C03/A11 销售查询及同筛选原金额/净额汇总")
  public ApiResponse<SalePageVO> page(
      @Valid @ModelAttribute SaleQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.page(query, user));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('sale.order')")
  @Operation(summary = "C03 销售快照详情，收银仅本人且不含成本")
  public ApiResponse<SaleDetailVO> detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.detail(id, user));
  }

  @GetMapping("/by-request/{key}")
  @PreAuthorize("hasAuthority('sale.order')")
  @Operation(summary = "C02 按本人提交标识恢复已完成结果，不重新扣库")
  public ApiResponse<SaleDetailVO> result(
      @PathVariable String key, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.byRequest(key, user));
  }

  @GetMapping("/context")
  @PreAuthorize("hasAuthority('sale.order')")
  @Operation(summary = "C01/C03 北京业务日及默认收款方式")
  public ApiResponse<SaleContextVO> context(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.context(user));
  }

  @GetMapping("/cashiers")
  @PreAuthorize("hasAuthority('sale.order')")
  @Operation(summary = "A11 管理员筛选收银员基本选项")
  public ApiResponse<List<CashierOptionVO>> cashiers(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.cashiers(user));
  }

  @PostMapping("/quote")
  @PreAuthorize("hasAuthority('sale.order.complete')")
  @Operation(summary = "C02 结算预校验，不占库存或生成销售")
  public ApiResponse<SaleQuoteVO> quote(
      @Valid @RequestBody SaleQuoteRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(quotes.quote(request, user));
  }

  @PostMapping("/complete")
  @PreAuthorize("hasAuthority('sale.order.complete')")
  @Operation(summary = "C02 幂等销售确认，FEFO/FIFO原子扣库")
  public ApiResponse<SaleDetailVO> complete(
      @Valid @RequestBody SaleCompleteRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(checkout.complete(request, user));
  }

  @PostMapping("/{id}/correct")
  @PreAuthorize("hasAuthority('sale.order.correct')")
  @Operation(summary = "A11 当天整单纠错，按原分配恢复库存")
  public ApiResponse<SaleDetailVO> correct(
      @PathVariable long id,
      @Valid @RequestBody SaleCorrectionRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(commands.correct(id, request, user));
  }
}
