package com.supermarket.inventory.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull Boolean enabled) {}
