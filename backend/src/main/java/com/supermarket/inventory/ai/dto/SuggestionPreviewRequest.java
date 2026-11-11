package com.supermarket.inventory.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.supermarket.inventory.ai.enums.SuggestionType;
import com.supermarket.inventory.common.IdInputDeserializer;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SuggestionPreviewRequest(
    @NotBlank @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
        String requestKey,
    @NotNull SuggestionType type,
    LocalDate startDate,
    LocalDate endDate,
    @JsonDeserialize(using = IdInputDeserializer.class) String categoryId,
    @JsonDeserialize(using = IdInputDeserializer.class) String productId,
    @Pattern(regexp = "qwen[a-zA-Z0-9.:-]{0,95}") String model) {}
