package com.supermarket.inventory.purchase.vo;

import com.supermarket.inventory.purchase.enums.ReconcileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ReconcileSnapshotVO(
    ReconcileStatus status,
    @Schema(type = "string") BigDecimal externalTotal,
    String reference,
    String difference,
    String resolution,
    List<Line> lines) {
  public record Line(
      String itemId,
      Integer externalQty,
      @Schema(type = "string") BigDecimal externalUnitCost,
      String note) {}
}
