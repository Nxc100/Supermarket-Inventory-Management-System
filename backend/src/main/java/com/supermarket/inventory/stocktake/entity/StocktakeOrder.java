package com.supermarket.inventory.stocktake.entity;

import com.supermarket.inventory.stocktake.enums.StocktakeStatus;
import java.time.LocalDateTime;

public record StocktakeOrder(
    Long id,
    String orderNo,
    Long operatorId,
    String operatorName,
    StocktakeStatus status,
    LocalDateTime startedAt,
    LocalDateTime submittedAt,
    Long reviewedBy,
    String reviewerName,
    LocalDateTime reviewedAt,
    String rejectionReason,
    LocalDateTime completedAt,
    Long closedBy,
    LocalDateTime closedAt,
    String closeReason,
    String remark,
    long rowVersion) {}
