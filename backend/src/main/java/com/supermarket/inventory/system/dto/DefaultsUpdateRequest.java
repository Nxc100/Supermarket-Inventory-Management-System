package com.supermarket.inventory.system.dto;

import jakarta.validation.constraints.*;

public record DefaultsUpdateRequest(
    @NotNull @Min(0) @Max(999999) Long stockLowerLimit,
    @NotNull @Min(0) @Max(3650) Integer nearExpiryDays,
    @NotNull @Min(0) @Max(999999) Long expectedStockLowerLimit,
    @NotNull @Min(0) @Max(3650) Integer expectedNearExpiryDays) {}
