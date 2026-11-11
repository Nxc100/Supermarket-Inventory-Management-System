package com.supermarket.inventory.system.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record StaffStateRequest(
    @NotNull Boolean enabled, @NotNull OffsetDateTime expectedUpdatedAt) {}
