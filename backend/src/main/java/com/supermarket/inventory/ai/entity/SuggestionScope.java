package com.supermarket.inventory.ai.entity;

import com.supermarket.inventory.ai.enums.SuggestionType;
import java.time.LocalDate;

public record SuggestionScope(
    SuggestionType type, LocalDate start, LocalDate end, String categoryId, String productId) {}
