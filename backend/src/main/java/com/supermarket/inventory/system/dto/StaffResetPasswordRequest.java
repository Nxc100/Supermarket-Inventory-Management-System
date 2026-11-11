package com.supermarket.inventory.system.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record StaffResetPasswordRequest(
    @NotBlank @Pattern(regexp = "(?=.*[A-Za-z])(?=.*[0-9]).{8,20}", message = "密码须为8～20位且包含字母与数字")
        String password,
    @NotBlank String confirmPassword,
    @NotNull OffsetDateTime expectedUpdatedAt) {
  @AssertTrue(message = "两次密码须一致")
  public boolean isPasswordMatching() {
    return password != null && password.equals(confirmPassword);
  }
}
