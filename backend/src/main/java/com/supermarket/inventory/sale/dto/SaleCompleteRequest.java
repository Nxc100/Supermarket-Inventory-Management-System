package com.supermarket.inventory.sale.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.MoneyInputDeserializer;
import com.supermarket.inventory.sale.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record SaleCompleteRequest(
    @NotBlank @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
        String requestKey,
    @NotNull PaymentMethod paymentMethod,
    @NotNull
        @DecimalMin("0.00")
        @JsonDeserialize(using = MoneyInputDeserializer.class)
        @Schema(type = "string", example = "100.00")
        BigDecimal receivedAmount,
    @NotEmpty @Size(max = 100) List<@NotNull @Valid SaleLineRequest> items) {}
