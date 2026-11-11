package com.supermarket.inventory.purchase.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.IdInputDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public record PurchaseSaveRequest(
    @NotBlank
        @Pattern(regexp = "[1-9][0-9]{0,18}")
        @JsonDeserialize(using = IdInputDeserializer.class)
        String supplierId,
    @Min(0) Long expectedVersion,
    LocalDate expectedArrivalDate,
    OffsetDateTime arrivedAt,
    @Size(max = 500) String remark,
    @NotEmpty @Size(max = 100) List<@NotNull @Valid PurchaseLineRequest> items) {}
