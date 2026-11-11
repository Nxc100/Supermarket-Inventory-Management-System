package com.supermarket.inventory.system.controller;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.service.SystemSettingsService;
import com.supermarket.inventory.system.vo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system")
public class SystemSettingsController {
  private final SystemSettingsService service;

  public SystemSettingsController(SystemSettingsService service) {
    this.service = service;
  }

  @GetMapping("/defaults")
  @PreAuthorize("hasAuthority('system.config')")
  public ApiResponse<SystemDefaultsVO> defaults(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.defaults(user));
  }

  @PutMapping("/defaults")
  @PreAuthorize("hasAuthority('system.config.save')")
  public ApiResponse<SystemDefaultsVO> save(
      @Valid @RequestBody DefaultsUpdateRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.save(request, user));
  }

  @GetMapping("/logs")
  @PreAuthorize("hasAuthority('system.log')")
  public ApiResponse<PageResult<OperationLogVO>> logs(
      @Valid @ModelAttribute OperationLogQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.logs(query, user));
  }

  @GetMapping("/logs/actions")
  @PreAuthorize("hasAuthority('system.log')")
  public ApiResponse<List<String>> actions(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.actions(user));
  }

  @GetMapping("/logs/{id}")
  @PreAuthorize("hasAuthority('system.log')")
  public ApiResponse<OperationLogDetailVO> detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.detail(id, user));
  }
}
