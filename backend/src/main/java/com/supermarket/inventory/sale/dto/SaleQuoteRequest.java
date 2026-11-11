package com.supermarket.inventory.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SaleQuoteRequest(
    @NotEmpty @Size(max = 100) List<@NotNull @Valid SaleLineRequest> items) {}
