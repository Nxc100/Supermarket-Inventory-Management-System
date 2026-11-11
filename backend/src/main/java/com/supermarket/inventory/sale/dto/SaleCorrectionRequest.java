package com.supermarket.inventory.sale.dto;

import jakarta.validation.constraints.*;

public record SaleCorrectionRequest(
    @NotNull @Min(0) Long expectedVersion,
    @NotNull @AssertTrue(message = "必须确认货物实际仍在或已完整收回") Boolean goodsReturned,
    @NotBlank @Size(min = 2, max = 500) String reason) {
  public SaleCorrectionRequest {
    if (reason != null) reason = reason.trim();
  }
}
