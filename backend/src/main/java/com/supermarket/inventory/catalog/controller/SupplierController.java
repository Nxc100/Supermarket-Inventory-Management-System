package com.supermarket.inventory.catalog.controller;

import com.supermarket.inventory.catalog.dto.*;
import com.supermarket.inventory.catalog.service.SupplierService;
import com.supermarket.inventory.catalog.vo.SupplierVO;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
  private final SupplierService service;

  public SupplierController(SupplierService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('base.supplier')")
  @Operation(summary = "A05/B01 共享供应商分页查询")
  public ApiResponse<PageResult<SupplierVO>> page(@Valid @ModelAttribute SupplierQuery query) {
    return ApiResponse.ok(service.page(query));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('base.supplier')")
  @Operation(summary = "A05/B01 供应商详情")
  public ApiResponse<SupplierVO> get(@PathVariable long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('base.supplier.create')")
  @Operation(summary = "A05/B01 新增供应商，系统生成编码")
  public ApiResponse<SupplierVO> create(
      @Valid @RequestBody SupplierRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.create(request, user));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('base.supplier.edit')")
  @Operation(summary = "A05/B01 编辑当前供应商档案")
  public ApiResponse<SupplierVO> update(
      @PathVariable long id,
      @Valid @RequestBody SupplierRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.update(id, request, user));
  }

  @PostMapping("/{id}/status")
  @PreAuthorize("hasAuthority('base.supplier.enable')")
  @Operation(summary = "A05/B01 供应商启停")
  public ApiResponse<SupplierVO> status(
      @PathVariable long id,
      @Valid @RequestBody StatusRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.status(id, request.enabled(), user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('base.supplier.delete')")
  @Operation(summary = "A05 管理员删除未引用供应商")
  public ApiResponse<Void> delete(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    service.delete(id, user);
    return ApiResponse.ok(null);
  }
}
