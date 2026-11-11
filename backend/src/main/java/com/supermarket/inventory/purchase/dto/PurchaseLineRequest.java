package com.supermarket.inventory.purchase.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseLineRequest(
    @NotBlank
        @Pattern(regexp = "[1-9][0-9]{0,18}")
        @JsonDeserialize(using = IdInputDeserializer.class)
        String productId,
    @NotNull @Min(1) @Max(999999) Integer quantity,
    @Min(0) @Max(999999) Integer receivedQty,
    @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @JsonDeserialize(using = MoneyInputDeserializer.class)
        @Schema(type = "string", example = "3.00")
        BigDecimal unitCost,
    @Size(max = 64) String supplierBatchNo,
    LocalDate productionDate,
    LocalDate expiryDate) {}
