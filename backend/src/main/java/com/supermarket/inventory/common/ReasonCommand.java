package com.supermarket.inventory.common;

import jakarta.validation.constraints.*;

public record ReasonCommand(
    @NotNull @Min(0) Long expectedVersion, @NotBlank @Size(min = 2, max = 500) String reason) {
  public ReasonCommand {
    if (reason != null) reason = reason.trim();
  }
}
