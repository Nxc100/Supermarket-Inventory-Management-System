package com.supermarket.inventory.ai.controller;

import com.supermarket.inventory.ai.dto.AiUsageQuery;
import com.supermarket.inventory.ai.service.*;
import com.supermarket.inventory.ai.vo.AiUsageVO;
import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/ai")
public class AiSettingsController {
  private final AiSettingsService settings;
  private final AiUsageService usage;

  public AiSettingsController(AiSettingsService settings, AiUsageService usage) {
    this.settings = settings;
    this.usage = usage;
  }

  @GetMapping("/settings")
  @PreAuthorize("hasAuthority('system.config')")
  public ResponseEntity<ApiResponse<AiSettingsService.SettingsVO>> get(
      @AuthenticationPrincipal CurrentUser user) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(settings.get(user)));
  }

  @PutMapping("/settings")
  @PreAuthorize("hasAuthority('system.config.save')")
  public ApiResponse<AiSettingsService.SettingsVO> save(
      @Valid @RequestBody AiSettingsService.SaveRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(settings.save(request, user));
  }

  @GetMapping("/usage")
  @PreAuthorize("hasAuthority('system.config')")
  public ApiResponse<AiUsageVO> usage(
      @Valid @ModelAttribute AiUsageQuery query, @AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(usage.usage(query, user));
  }
}
