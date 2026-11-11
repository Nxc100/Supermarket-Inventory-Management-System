package com.supermarket.inventory.system.controller;

import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.DashboardService;
import com.supermarket.inventory.system.vo.DashboardVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('common.dashboard')")
  public ApiResponse<DashboardVO> dashboard(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.dashboard(user));
  }
}
