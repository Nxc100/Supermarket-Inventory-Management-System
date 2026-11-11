package com.supermarket.inventory.ai.entity;

import com.supermarket.inventory.ai.vo.SuggestionPreviewVO;

public record PreparedSuggestion(
    long ownerId,
    SuggestionScope scope,
    SuggestionPreviewVO preview,
    String filtersJson,
    String summaryJson) {}
