package com.supermarket.inventory.system.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record StaffVersionRequest(@NotNull OffsetDateTime expectedUpdatedAt) {}
