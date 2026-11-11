package com.supermarket.inventory.purchase.dto;

import jakarta.validation.constraints.*;

public record ReconcileWithdrawRequest(
    @NotNull @Min(0) Long expectedVersion,
    @NotNull @Min(0) Integer expectedReconcileVersion,
    @NotBlank @Size(min = 2, max = 500) String reason) {
  public ReconcileWithdrawRequest {
    if (reason != null) reason = reason.trim();
  }
}
