package com.supermarket.inventory.purchase.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.purchase.dto.*;
import com.supermarket.inventory.purchase.service.*;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {
  private final PurchaseQueryService queries;
  private final PurchaseDraftService drafts;
  private final PurchaseWorkflowService workflow;
  private final PurchaseReconciliationService reconciliation;

  public PurchaseController(
      PurchaseQueryService queries,
      PurchaseDraftService drafts,
      PurchaseWorkflowService workflow,
      PurchaseReconciliationService reconciliation) {
    this.queries = queries;
    this.drafts = drafts;
    this.workflow = workflow;
    this.reconciliation = reconciliation;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('purchase.order')")
  @Operation(summary = "B03 分页查询本人采购，管理员查询全部")
  public ApiResponse<PageResult<PurchaseSummaryVO>> page(
      @Valid @ModelAttribute PurchaseQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.page(query, user));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase.order')")
  @Operation(summary = "B03 采购快照明细及核对历史")
  public ApiResponse<PurchaseDetailVO> detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(queries.detail(id, user));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('purchase.order.create')")
  @Operation(summary = "B02 新建采购草稿，不入库")
  public ApiResponse<PurchaseDetailVO> create(
      @Valid @RequestBody PurchaseSaveRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(drafts.create(request, user));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase.order.edit')")
  @Operation(summary = "B02/B03 更新草稿或驳回单，不入库")
  public ApiResponse<PurchaseDetailVO> update(
      @PathVariable long id,
      @Valid @RequestBody PurchaseSaveRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(drafts.update(id, request, user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase.order.delete_draft')")
  @Operation(summary = "B03 删除从未提交的本人草稿")
  public ApiResponse<Void> delete(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    drafts.delete(id, request.expectedVersion(), user);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAuthority('purchase.order.submit')")
  @Operation(summary = "B02 提交入库审核，不改库存")
  public ApiResponse<PurchaseDetailVO> submit(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(workflow.submit(id, request, user));
  }

  @PostMapping("/{id}/withdraw")
  @PreAuthorize("hasAuthority('purchase.order.withdraw')")
  @Operation(summary = "B03 撤回待审核单，保留提交历史")
  public ApiResponse<PurchaseDetailVO> withdraw(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(workflow.withdraw(id, request, user));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('purchase.order.review')")
  @Operation(summary = "A06 管理员审核，批次/余额/流水/状态同事务生效")
  public ApiResponse<PurchaseDetailVO> approve(
      @PathVariable long id,
      @Valid @RequestBody CommandVersion request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(workflow.approve(id, request, user));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('purchase.order.reject')")
  @Operation(summary = "A06 驳回采购，不改库存")
  public ApiResponse<PurchaseDetailVO> reject(
      @PathVariable long id,
      @Valid @RequestBody ReasonCommand request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(workflow.reject(id, request, user));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('purchase.order.close')")
  @Operation(summary = "B03 关闭草稿/驳回采购")
  public ApiResponse<PurchaseDetailVO> close(
      @PathVariable long id,
      @Valid @RequestBody ReasonCommand request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(workflow.close(id, request, user));
  }

  @PostMapping("/{id}/reverse")
  @PreAuthorize("hasAuthority('purchase.order.reverse')")
  @Operation(summary = "A07 受限整单撤销入库，生成反向流水")
  public ApiResponse<PurchaseDetailVO> reverse(
      @PathVariable long id,
      @Valid @RequestBody ReasonCommand request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(workflow.reverse(id, request, user));
  }

  @PostMapping("/{id}/reconcile-difference")
  @PreAuthorize("hasAuthority('purchase.order.reconcile')")
  @Operation(summary = "B03 登记逐行核对差异")
  public ApiResponse<PurchaseDetailVO> difference(
      @PathVariable long id,
      @Valid @RequestBody ReconcileSaveRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(reconciliation.save(id, request, user, false));
  }

  @PostMapping("/{id}/reconcile-match")
  @PreAuthorize("hasAuthority('purchase.order.reconcile')")
  @Operation(summary = "B03 确认逐行及合计一致")
  public ApiResponse<PurchaseDetailVO> match(
      @PathVariable long id,
      @Valid @RequestBody ReconcileSaveRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(reconciliation.save(id, request, user, true));
  }

  @PostMapping("/{id}/withdraw-reconciliation")
  @PreAuthorize("hasAuthority('purchase.order.reconcile')")
  @Operation(summary = "B03 撤回本次核对，保留完整历史快照")
  public ApiResponse<PurchaseDetailVO> withdrawReconciliation(
      @PathVariable long id,
      @Valid @RequestBody ReconcileWithdrawRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(reconciliation.withdraw(id, request, user));
  }
}
