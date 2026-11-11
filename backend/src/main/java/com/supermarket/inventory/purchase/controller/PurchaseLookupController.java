package com.supermarket.inventory.purchase.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.purchase.dto.SupplierSummaryQuery;
import com.supermarket.inventory.purchase.service.PurchaseLookupService;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseLookupController {
  private final PurchaseLookupService service;

  public PurchaseLookupController(PurchaseLookupService service) {
    this.service = service;
  }

  @GetMapping("/context")
  @PreAuthorize("hasAuthority('purchase.order')")
  @Operation(summary = "B02 采购北京时间与默认核对区间")
  public ApiResponse<PurchaseContextVO> context(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.context(user));
  }

  @GetMapping("/supplier-options")
  @PreAuthorize("hasAuthority('purchase.order')")
  @Operation(summary = "B02 分页检索供应商基本选项")
  public ApiResponse<PageResult<SupplierOptionVO>> suppliers(
      @Valid @ModelAttribute PageQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.suppliers(query, user));
  }

  @GetMapping("/supplier-summary")
  @PreAuthorize("hasAuthority('purchase.order.reconcile')")
  @Operation(summary = "B03 按入库/撤销事件汇总本人或全店供应商净采购")
  public ApiResponse<PageResult<SupplierPurchaseSummaryVO>> summary(
      @Valid @ModelAttribute SupplierSummaryQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.summary(query, user));
  }
}
