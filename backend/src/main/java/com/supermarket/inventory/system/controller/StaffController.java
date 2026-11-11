package com.supermarket.inventory.system.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.service.StaffService;
import com.supermarket.inventory.system.vo.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/users")
@PreAuthorize("hasAuthority('system.user')")
public class StaffController {
  private final StaffService service;

  public StaffController(StaffService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<StaffVO>> page(
      @Valid @ModelAttribute StaffQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.page(query, user));
  }

  @GetMapping("/{id}")
  public ApiResponse<StaffVO> detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.detail(id, user));
  }

  @GetMapping("/{id}/tasks")
  public ApiResponse<PageResult<StaffTaskVO>> tasks(
      @PathVariable long id,
      @Valid @ModelAttribute PageQuery query,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.tasks(id, query, user));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('system.user.create')")
  public ApiResponse<StaffVO> create(
      @Valid @RequestBody StaffCreateRequest request, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.create(request, user));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system.user.edit')")
  public ApiResponse<StaffVO> update(
      @PathVariable long id,
      @Valid @RequestBody StaffUpdateRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.update(id, request, user));
  }

  @PatchMapping("/{id}/state")
  @PreAuthorize("hasAuthority('system.user.enable')")
  public ApiResponse<StaffVO> state(
      @PathVariable long id,
      @Valid @RequestBody StaffStateRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.state(id, request, user));
  }

  @PostMapping("/{id}/reset-password")
  @PreAuthorize("hasAuthority('system.user.reset_password')")
  public ApiResponse<StaffVO> reset(
      @PathVariable long id,
      @Valid @RequestBody StaffResetPasswordRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.reset(id, request, user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system.user.delete')")
  public ApiResponse<Void> delete(
      @PathVariable long id,
      @Valid @RequestBody StaffVersionRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    service.delete(id, request, user);
    return ApiResponse.ok(null);
  }
}
