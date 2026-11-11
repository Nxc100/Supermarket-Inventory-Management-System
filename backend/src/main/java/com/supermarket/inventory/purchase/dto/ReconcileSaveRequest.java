package com.supermarket.inventory.purchase.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ReconcileSaveRequest(
    @NotNull @Min(0) Long expectedVersion,
    @NotNull @Min(0) Integer expectedReconcileVersion,
    @NotNull
        @DecimalMin("0.00")
        @JsonDeserialize(using = MoneyInputDeserializer.class)
        @Schema(type = "string", example = "60.00")
        BigDecimal externalTotal,
    @Size(max = 100) String reference,
    @Size(max = 500) String difference,
    @Size(max = 500) String resolution,
    @NotEmpty @Size(max = 100) List<@NotNull @Valid Line> lines) {
  public record Line(
      @NotBlank @JsonDeserialize(using = IdInputDeserializer.class) String itemId,
      @NotNull @Min(0) @Max(999999) Integer externalQty,
      @NotNull
          @DecimalMin("0.00")
          @JsonDeserialize(using = MoneyInputDeserializer.class)
          @Schema(type = "string", example = "3.00")
          BigDecimal externalUnitCost,
      @Size(max = 500) String note) {}
}
