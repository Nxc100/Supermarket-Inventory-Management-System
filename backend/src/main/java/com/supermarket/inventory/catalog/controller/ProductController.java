package com.supermarket.inventory.catalog.controller;

import com.supermarket.inventory.catalog.dto.*;
import com.supermarket.inventory.catalog.service.*;
import com.supermarket.inventory.catalog.vo.*;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
  private final ProductService service;
  private final ProductImageService images;

  public ProductController(ProductService service, ProductImageService images) {
    this.service = service;
    this.images = images;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('base.product')")
  @Operation(summary = "A04 管理员商品档案分页")
  public ApiResponse<PageResult<ProductVO>> page(@Valid @ModelAttribute ProductQuery query) {
    return ApiResponse.ok(service.page(query));
  }

  @GetMapping("/defaults")
  @PreAuthorize("hasAuthority('base.product')")
  @Operation(summary = "A04 新商品默认参数与基本单位")
  public ApiResponse<ProductDefaultsVO> defaults() {
    return ApiResponse.ok(service.defaults());
  }

  @GetMapping("/category-options")
  @PreAuthorize(
      "hasAuthority('base.product') or hasAuthority('product.purchase_lookup') or hasAuthority('product.sale_lookup') or hasAuthority('inventory.query')")
  @Operation(summary = "A03/A04 分类选择，只返回分类基本资料")
  public ApiResponse<List<CategoryOptionVO>> options() {
    return ApiResponse.ok(service.categoryOptions());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('base.product')")
  @Operation(summary = "A04 商品档案详情")
  public ApiResponse<ProductVO> get(@PathVariable long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('base.product.create')")
  @Operation(summary = "A04 新增商品，库存初始为零")
  public ApiResponse<ProductVO> create(
      @Valid @RequestBody ProductRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.create(request, user));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('base.product.edit')")
  @Operation(summary = "A04 编辑档案，保留业务快照与库存")
  public ApiResponse<ProductVO> update(
      @PathVariable long id,
      @Valid @RequestBody ProductRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.update(id, request, user));
  }

  @PostMapping("/{id}/status")
  @PreAuthorize("hasAuthority('base.product.enable')")
  @Operation(summary = "A04 商品启停")
  public ApiResponse<ProductVO> status(
      @PathVariable long id,
      @Valid @RequestBody StatusRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.status(id, request.enabled(), user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('base.product.delete')")
  @Operation(summary = "A04 删除无库存且无业务引用的商品")
  public ApiResponse<Void> delete(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    images.delete(id, user);
    return ApiResponse.ok(null);
  }

  @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
  @PreAuthorize("hasAuthority('base.product.image')")
  @Operation(summary = "A04 验证并绑定商品图片")
  public ApiResponse<ProductVO> image(
      @PathVariable long id,
      @RequestPart("file") MultipartFile file,
      MultipartHttpServletRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    if (!request.getParameterMap().isEmpty()
        || request.getFileMap().size() != 1
        || request.getFiles("file").size() != 1)
      throw new BusinessException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "只接受单张file图片");
    return ApiResponse.ok(images.upload(id, file, user));
  }

  @DeleteMapping("/{id}/image")
  @PreAuthorize("hasAuthority('base.product.image')")
  @Operation(summary = "A04 移除商品图片，保留其他用户/商品共享文件")
  public ApiResponse<ProductVO> removeImage(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(images.remove(id, user));
  }
}
