package com.supermarket.inventory.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.*;

public record StockBatchVO(
    String id,
    String batchNo,
    String supplierBatchNo,
    String purchaseId,
    String purchaseNo,
    boolean sourceReadable,
    String sourceStatus,
    LocalDate productionDate,
    LocalDate expiryDate,
    Long remainingDays,
    long stockQty,
    @Schema(type = "string", example = "8.00") BigDecimal unitCost,
    boolean saleable,
    boolean frozen,
    boolean enabled,
    boolean canSell,
    LocalDateTime stockedAt) {}
