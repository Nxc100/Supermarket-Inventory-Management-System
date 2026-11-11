package com.supermarket.inventory.ai.vo;

import com.supermarket.inventory.ai.enums.*;
import java.time.*;

public record SuggestionSummaryVO(
    String id,
    SuggestionType type,
    String operatorName,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime snapshotAt,
    String provider,
    String modelName,
    boolean fixture,
    SuggestionStatus status,
    LocalDateTime requestedAt,
    LocalDateTime finishedAt,
    String errorMessage) {}
