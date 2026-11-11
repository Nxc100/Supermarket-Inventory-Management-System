package com.supermarket.inventory.stocktake.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.common.IdInputDeserializer;
import com.supermarket.inventory.stocktake.enums.CountReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record StocktakeSaveRequest(
    @NotNull @Min(0) Long expectedVersion,
    @NotNull @Size(min = 1, max = 2000) List<@Valid Line> items) {
  public record Line(
      @NotBlank @JsonDeserialize(using = IdInputDeserializer.class) String id,
      @Min(0) @Max(999999999999L) Long actualQty,
      CountReason reasonType,
      @Size(max = 500) String reasonNote) {
    public Line {
      if (reasonNote != null) reasonNote = reasonNote.trim();
    }
  }
}
