package com.supermarket.inventory.profile.dto;

import jakarta.validation.constraints.*;

public record PasswordChangeRequest(
    @NotBlank @Size(max = 100) String oldPassword,
    @NotBlank @Pattern(regexp = "(?=.*[A-Za-z])(?=.*[0-9]).{8,20}", message = "新密码须为8～20位且包含字母与数字")
        String newPassword,
    @NotBlank String confirmPassword) {
  @AssertTrue(message = "两次新密码须一致，且不得与旧密码相同")
  public boolean isPasswordValid() {
    return newPassword != null
        && newPassword.equals(confirmPassword)
        && !newPassword.equals(oldPassword);
  }
}
