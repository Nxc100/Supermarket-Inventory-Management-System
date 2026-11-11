package com.supermarket.inventory.ai.vo;

import java.time.*;
import java.util.List;

public record AiUsageVO(
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime collectedAt,
    UsageRow totals,
    List<UsageRow> models,
    List<UsageRow> days) {
  public record UsageRow(
      String label,
      long calls,
      long successes,
      long failures,
      long pending,
      long reportedCalls,
      long unreportedCalls,
      long promptTokens,
      long completionTokens,
      long totalTokens,
      long cachedTokens,
      long cacheReportedCalls) {}
}
