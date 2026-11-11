package com.supermarket.inventory.stocktake.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.stocktake.dto.*;
import com.supermarket.inventory.stocktake.service.StocktakeService;
import com.supermarket.inventory.stocktake.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocktakes")
public class StocktakeController {
  private final StocktakeService service;

  public StocktakeController(StocktakeService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory.stocktake')")
  @Operation(summary = "A09/B04 本人盘点列表，管理员全部")
  public ApiResponse<PageResult<StocktakeSummaryVO>> page(
      @Valid @ModelAttribute StocktakeQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.page(query, user));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('inventory.stocktake')")
  @Operation(summary = "A09/B04 批次快照及空白实盘数")
  public ApiResponse<StocktakeDetailVO> detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.detail(id, user));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('inventory.stocktake.start')")
  @Operation(summary = "B04 原子开始盘点、冻结商品并保存批次快照")
  public ApiResponse<StocktakeDetailVO> start(
      @Valid @RequestBody StocktakeStartRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.start(request, user));
  }

  @PutMapping("/{id}/counts")
  @PreAuthorize("hasAuthority('inventory.stocktake.save')")
  @Operation(summary = "B04 暂存全部实盘行，未清点保留null")
  public ApiResponse<StocktakeDetailVO> save(
      @PathVariable long id,
      @Valid @RequestBody StocktakeSaveRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.save(id, request, user));
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAuthority('inventory.stocktake.submit')")
  @Operation(summary = "B04 全部清点后提交审核，继续冻结")
  public ApiResponse<StocktakeDetailVO> submit(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.submit(id, request, user));
  }

  @PostMapping("/{id}/withdraw")
  @PreAuthorize("hasAuthority('inventory.stocktake.withdraw')")
  @Operation(summary = "B04 撤回待审核盘点，继续冻结")
  public ApiResponse<StocktakeDetailVO> withdraw(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.withdraw(id, request, user));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('inventory.stocktake.review')")
  @Operation(summary = "A09 核查冻结和快照、原子调整库存并释放")
  public ApiResponse<StocktakeDetailVO> approve(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.approve(id, request, user));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('inventory.stocktake.reject')")
  @Operation(summary = "A09 驳回仍保留冻结")
  public ApiResponse<StocktakeDetailVO> reject(
      @PathVariable long id,
      @Valid @RequestBody ReasonCommand request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.reject(id, request, user));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('inventory.stocktake.close')")
  @Operation(summary = "A09/B04 关闭未完成盘点，释放本单冻结且不调整库存")
  public ApiResponse<StocktakeDetailVO> close(
      @PathVariable long id,
      @Valid @RequestBody ReasonCommand request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.close(id, request, user));
  }
}
