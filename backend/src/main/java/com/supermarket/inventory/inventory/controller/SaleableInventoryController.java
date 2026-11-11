package com.supermarket.inventory.inventory.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.inventory.dto.InventoryQuery;
import com.supermarket.inventory.inventory.service.SaleableInventoryService;
import com.supermarket.inventory.inventory.vo.CashierInventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/saleable-products")
public class SaleableInventoryController {
  private final SaleableInventoryService service;

  public SaleableInventoryController(SaleableInventoryService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory.query')")
  @Operation(summary = "C04 只读商品库存，按安全投影返回")
  public ApiResponse<PageResult<CashierInventoryVO>> page(
      @Valid @ModelAttribute InventoryQuery query) {
    return ApiResponse.ok(service.page(query));
  }
}
