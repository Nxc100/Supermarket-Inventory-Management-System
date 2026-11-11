package com.supermarket.inventory.ai.vo;

import com.fasterxml.jackson.databind.JsonNode;

public record SuggestionDetailVO(
    SuggestionSummaryVO record,
    JsonNode filters,
    JsonNode summary,
    String content,
    String errorCode,
    Integer durationMs,
    boolean recoverablePending,
    com.supermarket.inventory.ai.client.TokenUsage usage) {}
