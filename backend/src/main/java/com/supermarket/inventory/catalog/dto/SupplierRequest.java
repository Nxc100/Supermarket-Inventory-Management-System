package com.supermarket.inventory.catalog.dto;

import jakarta.validation.constraints.*;

public record SupplierRequest(
    @NotBlank @Size(min = 2, max = 100) String name,
    @NotBlank @Size(min = 2, max = 30) String contactName,
    @NotBlank @Pattern(regexp = "[0-9][0-9 -]{4,18}[0-9]", message = "电话须为6～20位数字、空格或短横线")
        String contactPhone,
    @Size(max = 200) String address,
    @Size(max = 500) String remark) {
  public SupplierRequest {
    if (name != null) name = name.trim();
    if (contactName != null) contactName = contactName.trim();
    if (contactPhone != null) contactPhone = contactPhone.trim();
  }
}
