package com.supermarket.inventory.catalog.controller;

import com.supermarket.inventory.catalog.dto.ProductQuery;
import com.supermarket.inventory.catalog.service.ProductService;
import com.supermarket.inventory.catalog.vo.*;
import com.supermarket.inventory.common.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/product-lookups")
public class ProductLookupController {
  private final ProductService service;

  public ProductLookupController(ProductService service) {
    this.service = service;
  }

  @GetMapping("/purchase")
  @PreAuthorize("hasAuthority('product.purchase_lookup')")
  @Operation(summary = "B02 采购选品，排除停用及禁止采购商品")
  public ApiResponse<PageResult<PurchaseProductVO>> purchase(
      @Valid @ModelAttribute ProductQuery query) {
    return ApiResponse.ok(service.purchaseLookup(query));
  }

  @GetMapping("/sale")
  @PreAuthorize("hasAuthority('product.sale_lookup')")
  @Operation(summary = "C01 销售选品，永不返回采购价或来源")
  public ApiResponse<PageResult<SaleProductVO>> sale(@Valid @ModelAttribute ProductQuery query) {
    return ApiResponse.ok(service.saleLookup(query));
  }
}
