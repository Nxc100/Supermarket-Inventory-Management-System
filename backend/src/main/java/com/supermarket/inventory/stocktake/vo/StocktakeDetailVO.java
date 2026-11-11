package com.supermarket.inventory.stocktake.vo;

import java.time.LocalDateTime;
import java.util.List;

public record StocktakeDetailVO(
    StocktakeSummaryVO order,
    List<StocktakeItemVO> items,
    String remark,
    String reviewerName,
    LocalDateTime reviewedAt,
    String rejectionReason,
    LocalDateTime closedAt,
    String closeReason,
    boolean activeFreeze) {}
