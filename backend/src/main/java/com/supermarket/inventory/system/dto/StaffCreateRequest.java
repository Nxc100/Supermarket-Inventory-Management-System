package com.supermarket.inventory.system.dto;

import com.supermarket.inventory.system.enums.StaffRole;
import jakarta.validation.constraints.*;

public record StaffCreateRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{4,20}", message = "用户名须为4～20位字母、数字或下划线")
        String username,
    @NotBlank @Size(min = 2, max = 20) String realName,
    @Pattern(regexp = "1[3-9][0-9]{9}", message = "请输入11位中国大陆手机号") String phone,
    @NotNull StaffRole role,
    @NotBlank @Pattern(regexp = "(?=.*[A-Za-z])(?=.*[0-9]).{8,20}", message = "密码须为8～20位且包含字母与数字")
        String password,
    @NotBlank String confirmPassword) {
  public StaffCreateRequest {
    if (username != null) username = username.strip();
    if (realName != null) realName = realName.strip();
    if (phone != null) phone = phone.isBlank() ? null : phone.strip();
  }

  @AssertTrue(message = "两次密码须一致")
  public boolean isPasswordMatching() {
    return password != null && password.equals(confirmPassword);
  }
}
