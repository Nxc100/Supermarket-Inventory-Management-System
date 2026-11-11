package com.supermarket.inventory.catalog.controller;

import com.supermarket.inventory.catalog.dto.*;
import com.supermarket.inventory.catalog.service.CategoryService;
import com.supermarket.inventory.catalog.vo.CategoryVO;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
  private final CategoryService service;

  public CategoryController(CategoryService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('base.category')")
  @Operation(summary = "A03 分页查询分类")
  public ApiResponse<PageResult<CategoryVO>> page(@Valid @ModelAttribute PageQuery query) {
    return ApiResponse.ok(service.page(query));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('base.category')")
  @Operation(summary = "A03 分类详情")
  public ApiResponse<CategoryVO> get(@PathVariable long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('base.category.create')")
  @Operation(summary = "A03 新增分类，默认启用")
  public ApiResponse<CategoryVO> create(
      @Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.create(request, user));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('base.category.edit')")
  @Operation(summary = "A03 编辑当前分类档案")
  public ApiResponse<CategoryVO> update(
      @PathVariable long id,
      @Valid @RequestBody CategoryRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.update(id, request, user));
  }

  @PostMapping("/{id}/status")
  @PreAuthorize("hasAuthority('base.category.enable')")
  @Operation(summary = "A03 分类启停，不级联停用商品")
  public ApiResponse<CategoryVO> status(
      @PathVariable long id,
      @Valid @RequestBody StatusRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.status(id, request.enabled(), user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('base.category.delete')")
  @Operation(summary = "A03 删除无商品引用的分类")
  public ApiResponse<Void> delete(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    service.delete(id, user);
    return ApiResponse.ok(null);
  }
}
