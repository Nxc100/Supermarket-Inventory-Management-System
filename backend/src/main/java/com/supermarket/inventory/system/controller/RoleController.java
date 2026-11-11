package com.supermarket.inventory.system.controller;

import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.enums.StaffRole;
import com.supermarket.inventory.system.service.RoleService;
import com.supermarket.inventory.system.vo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/roles")
@PreAuthorize("hasAuthority('system.role')")
public class RoleController {
  private final RoleService service;

  public RoleController(RoleService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<RoleSummaryVO>> all(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.all(user));
  }

  @GetMapping("/{role}")
  public ApiResponse<RoleDetailVO> detail(
      @PathVariable StaffRole role, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.detail(role, user));
  }

  @PutMapping("/{role}/permissions")
  @PreAuthorize("hasAuthority('system.role.save')")
  public ApiResponse<RoleDetailVO> save(
      @PathVariable StaffRole role,
      @Valid @RequestBody RoleSaveRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.save(role, request, user));
  }

  @PostMapping("/{role}/restore")
  @PreAuthorize("hasAuthority('system.role.restore')")
  public ApiResponse<RoleDetailVO> restore(
      @PathVariable StaffRole role,
      @Valid @RequestBody RoleRestoreRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.restore(role, request, user));
  }
}
