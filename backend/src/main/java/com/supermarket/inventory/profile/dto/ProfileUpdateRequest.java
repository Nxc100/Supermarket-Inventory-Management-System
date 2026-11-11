package com.supermarket.inventory.profile.dto;

import jakarta.validation.constraints.*;

public record ProfileUpdateRequest(
    @NotBlank @Size(min = 2, max = 20) String realName,
    @Pattern(regexp = "1[3-9][0-9]{9}", message = "请输入11位中国大陆手机号") String phone) {
  public ProfileUpdateRequest {
    if (realName != null) realName = realName.trim();
    if (phone != null) phone = phone.isBlank() ? null : phone.trim();
  }
}
