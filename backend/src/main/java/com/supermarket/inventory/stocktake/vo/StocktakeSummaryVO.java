package com.supermarket.inventory.stocktake.vo;

import com.supermarket.inventory.stocktake.enums.StocktakeStatus;
import java.time.LocalDateTime;

public record StocktakeSummaryVO(
    String id,
    String orderNo,
    String operatorId,
    String operatorName,
    StocktakeStatus status,
    LocalDateTime startedAt,
    LocalDateTime submittedAt,
    LocalDateTime completedAt,
    long rowVersion,
    int productCount,
    int batchCount,
    int countedCount,
    long bookQty,
    Long actualQty,
    Long differenceQty) {}
