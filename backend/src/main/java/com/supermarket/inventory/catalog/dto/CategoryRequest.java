package com.supermarket.inventory.catalog.dto;

import jakarta.validation.constraints.*;

public record CategoryRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{2,20}", message = "编码须为2～20位字母数字或下划线") String code,
    @NotBlank @Size(min = 2, max = 30) String name,
    @NotNull @Min(0) Integer sortOrder) {
  public CategoryRequest {
    if (code != null) code = code.trim();
    if (name != null) name = name.trim();
  }
}
