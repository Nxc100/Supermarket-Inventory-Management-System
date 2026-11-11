package com.supermarket.inventory.security;

import com.supermarket.inventory.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/login")
  @Operation(summary = "G01 三角色登录")
  public ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(service.login(request));
  }

  @GetMapping("/me")
  @Operation(summary = "G01 获取当前用户及有效权限")
  public ApiResponse<UserVO> me(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(service.me(user.id()));
  }

  @PostMapping("/logout")
  @Operation(summary = "G01 退出并使本人全部会话失效")
  @PreAuthorize("hasAuthority('common.logout')")
  public ApiResponse<Void> logout(@AuthenticationPrincipal CurrentUser user) {
    service.logout(user.id());
    return ApiResponse.ok(null);
  }
}
