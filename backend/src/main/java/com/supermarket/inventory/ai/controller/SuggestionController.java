package com.supermarket.inventory.ai.controller;

import com.supermarket.inventory.ai.dto.*;
import com.supermarket.inventory.ai.service.SuggestionService;
import com.supermarket.inventory.ai.vo.*;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suggestions")
@PreAuthorize("hasAnyAuthority('ai.inventory','ai.sales')")
public class SuggestionController {
  @GetMapping("/category-options")
  @Operation(summary = "A13/B06 建议范围的安全分类选项，独立于采购和商品维护权限")
  public ApiResponse<java.util.List<com.supermarket.inventory.catalog.vo.CategoryOptionVO>>
      categories(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.categoryOptions(user));
  }

  @GetMapping("/product-options")
  @Operation(summary = "A13/B06 类型授权的安全选品，不返回采购成本或来源")
  public ApiResponse<PageResult<SuggestionProductOptionVO>> products(
      @Valid @ModelAttribute SuggestionProductQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.productOptions(query, user));
  }

  private final SuggestionService service;

  public SuggestionController(SuggestionService service) {
    this.service = service;
  }

  @GetMapping("/context")
  @Operation(summary = "A13/B06 权限内建议类型、默认区间和模型标识")
  public ApiResponse<SuggestionService.Context> context(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.context(user));
  }

  @PostMapping("/preview")
  @Operation(summary = "A13/B06 真实聚合快照，服务端保留10分钟供确认，不调用模型")
  public ApiResponse<SuggestionPreviewVO> preview(
      @Valid @RequestBody SuggestionPreviewRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.preview(request, user));
  }

  @PostMapping("/by-request/{key}/generate")
  @Operation(summary = "A13/B06 仅确认过的本人快照调用模型，同键不重复调用，基础事务独立")
  public ApiResponse<SuggestionDetailVO> generate(
      @PathVariable String key,
      @Valid @RequestBody SuggestionConfirmRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.generate(key, user));
  }

  @GetMapping("/by-request/{key}")
  @Operation(summary = "A13/B06 本人本次结果核实，绝不重新调用模型")
  public ApiResponse<SuggestionDetailVO> byKey(
      @PathVariable String key, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.byKey(key, user));
  }

  @GetMapping
  @Operation(summary = "A13/B06 类型授权历史，采购共享库存成功记录及本人失败/待完成记录")
  public ApiResponse<PageResult<SuggestionSummaryVO>> page(
      @Valid @ModelAttribute SuggestionQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.page(query, user));
  }

  @GetMapping("/{id}")
  @Operation(summary = "A13/B06 历史快照、真实/夹具来源、正文或安全失败原因")
  public ApiResponse<SuggestionDetailVO> detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.detail(id, user));
  }
}
