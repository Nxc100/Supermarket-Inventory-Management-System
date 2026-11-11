package com.supermarket.inventory.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 20) String username, @NotBlank @Size(max = 100) String password) {
  public LoginRequest {
    if (username != null) username = username.trim();
  }
}
