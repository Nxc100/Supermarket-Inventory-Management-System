package com.supermarket.inventory.system.dto;

import com.supermarket.inventory.system.enums.StaffRole;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record StaffUpdateRequest(
    @NotBlank @Size(min = 2, max = 20) String realName,
    @Pattern(regexp = "1[3-9][0-9]{9}", message = "请输入11位中国大陆手机号") String phone,
    @NotNull StaffRole role,
    @NotNull OffsetDateTime expectedUpdatedAt) {
  public StaffUpdateRequest {
    if (realName != null) realName = realName.strip();
    if (phone != null) phone = phone.isBlank() ? null : phone.strip();
  }
}
