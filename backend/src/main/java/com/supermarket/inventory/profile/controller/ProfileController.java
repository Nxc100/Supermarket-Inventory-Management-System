package com.supermarket.inventory.profile.controller;

import com.supermarket.inventory.common.ApiResponse;
import com.supermarket.inventory.profile.dto.*;
import com.supermarket.inventory.profile.service.AvatarService;
import com.supermarket.inventory.profile.service.ProfileService;
import com.supermarket.inventory.security.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
  private final ProfileService service;
  private final AvatarService avatars;

  public ProfileController(ProfileService service, AvatarService avatars) {
    this.service = service;
    this.avatars = avatars;
  }

  @PatchMapping
  @PreAuthorize("hasAuthority('common.profile')")
  @Operation(summary = "G03 更新本人姓名、手机号")
  public ApiResponse<UserVO> update(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ProfileUpdateRequest request) {
    return ApiResponse.ok(service.update(user.id(), request));
  }

  @PostMapping("/password")
  @PreAuthorize("hasAuthority('common.password')")
  @Operation(summary = "G05 修改本人密码并失效旧凭证")
  public ApiResponse<Void> password(
      @AuthenticationPrincipal CurrentUser user,
      @Valid @RequestBody PasswordChangeRequest request) {
    service.changePassword(user.id(), request);
    return ApiResponse.ok(null);
  }

  @PostMapping(value = "/avatar", consumes = "multipart/form-data")
  @PreAuthorize("hasAuthority('common.avatar')")
  @Operation(summary = "G04 验证并绑定本人头像，不接受用户ID或保存路径")
  public ApiResponse<UserVO> avatar(
      @AuthenticationPrincipal CurrentUser user,
      @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
      org.springframework.web.multipart.MultipartHttpServletRequest request) {
    if (!request.getParameterMap().isEmpty()
        || request.getFileMap().size() != 1
        || request.getFiles("file").size() != 1) {
      throw new com.supermarket.inventory.common.BusinessException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "VALIDATION_ERROR",
          "头像上传只接受一张file图片，不接受用户ID或路径");
    }
    return ApiResponse.ok(avatars.upload(user.id(), file));
  }

  @DeleteMapping("/avatar")
  @PreAuthorize("hasAuthority('common.avatar')")
  @Operation(summary = "G04 使用默认头像并安全回收无引用文件")
  public ApiResponse<UserVO> removeAvatar(@AuthenticationPrincipal CurrentUser user) {
    return ApiResponse.ok(avatars.remove(user.id()));
  }
}
