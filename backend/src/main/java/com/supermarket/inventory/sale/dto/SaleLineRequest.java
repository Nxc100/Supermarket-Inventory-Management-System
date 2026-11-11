package com.supermarket.inventory.sale.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SaleLineRequest(
    @NotBlank @JsonDeserialize(using = IdInputDeserializer.class) String productId,
    @NotNull @Min(1) @Max(999999) Integer quantity,
    @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @JsonDeserialize(using = MoneyInputDeserializer.class)
        @Schema(type = "string", example = "5.00")
        BigDecimal expectedPrice) {}
