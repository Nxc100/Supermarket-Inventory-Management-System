package com.supermarket.inventory.ai.entity;

import com.supermarket.inventory.ai.enums.*;
import java.time.*;

public record SuggestionRecord(
    Long id,
    String requestKey,
    SuggestionType suggestionType,
    Long operatorId,
    String operatorName,
    LocalDate periodStart,
    LocalDate periodEnd,
    String filterData,
    String inputSummary,
    LocalDateTime snapshotAt,
    String provider,
    String modelName,
    boolean isFixture,
    SuggestionStatus status,
    String content,
    String errorCode,
    String errorMessage,
    Integer durationMs,
    LocalDateTime requestedAt,
    LocalDateTime finishedAt,
    Long promptTokens,
    Long completionTokens,
    Long totalTokens,
    Long cachedTokens,
    String providerRequestId) {}
