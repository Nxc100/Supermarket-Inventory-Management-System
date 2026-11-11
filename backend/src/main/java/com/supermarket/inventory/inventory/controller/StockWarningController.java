package com.supermarket.inventory.inventory.controller;

import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.inventory.dto.WarningQuery;
import com.supermarket.inventory.inventory.service.StockWarningService;
import com.supermarket.inventory.inventory.vo.WarningPageVO;
import com.supermarket.inventory.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/warnings")
public class StockWarningController {
  private final StockWarningService service;

  public StockWarningController(StockWarningService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory.warning')")
  @Operation(summary = "A10 业务日实时计算低库存、临期和过期，只读且按岗位授权")
  public ApiResponse<WarningPageVO> page(
      @Valid @ModelAttribute WarningQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.page(query, user));
  }
}
