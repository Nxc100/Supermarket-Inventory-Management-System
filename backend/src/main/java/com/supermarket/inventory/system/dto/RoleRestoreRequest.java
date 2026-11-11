package com.supermarket.inventory.system.dto;

import jakarta.validation.constraints.*;

public record RoleRestoreRequest(@NotNull @Min(0) Long expectedVersion) {}
