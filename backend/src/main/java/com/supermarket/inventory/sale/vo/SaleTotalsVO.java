package com.supermarket.inventory.sale.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record SaleTotalsVO(
    @Schema(type = "string") BigDecimal originalAmount,
    @Schema(type = "string") BigDecimal netAmount,
    long completedCount,
    long correctedCount) {}
