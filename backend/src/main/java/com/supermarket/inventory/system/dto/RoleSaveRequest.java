package com.supermarket.inventory.system.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record RoleSaveRequest(
    @NotNull @Min(0) Long expectedVersion,
    @NotNull @Size(max = 200)
        List<@NotBlank @Pattern(regexp = "[a-z][a-z0-9_.]{0,79}") String> permissions) {}
