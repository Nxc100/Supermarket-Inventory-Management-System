package com.supermarket.inventory.stocktake.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.IdInputDeserializer;
import jakarta.validation.constraints.*;
import java.util.List;

public record StocktakeStartRequest(
    @NotNull @Size(min = 1, max = 100) @JsonDeserialize(contentUsing = IdInputDeserializer.class)
        List<@NotBlank String> productIds,
    @Size(max = 500) String remark) {
  public StocktakeStartRequest {
    if (remark != null) remark = remark.trim();
  }
}
