package com.supermarket.inventory.ai.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.supermarket.inventory.ai.enums.SuggestionType;
import java.time.*;

public record SuggestionPreviewVO(
    String requestKey,
    SuggestionType type,
    LocalDate periodStart,
    LocalDate periodEnd,
    String categoryId,
    String productId,
    LocalDateTime snapshotAt,
    LocalDateTime expiresAt,
    String provider,
    String model,
    JsonNode summary) {}
