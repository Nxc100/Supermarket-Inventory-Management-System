package com.supermarket.inventory.common;

import jakarta.validation.constraints.*;

public record CommandVersion(@NotNull @Min(0) Long expectedVersion) {}
