package com.supermarket.inventory.ai.client;

import com.supermarket.inventory.ai.enums.SuggestionType;

/**
 * The only external model boundary. Implementations never receive inventory writers or user data.
 */
public interface SuggestionClient {
  record Identity(String provider, String model, boolean fixture) {}

  record Result(String content, Identity identity, TokenUsage usage) {
    public Result(String content, Identity identity) {
      this(content, identity, TokenUsage.unknown());
    }
  }

  Identity identity();

  Result generate(SuggestionType type, String summary);

  default Result stream(
      SuggestionType type,
      String summary,
      String model,
      java.util.function.Consumer<String> delta) {
    Result result = generate(type, summary);
    delta.accept(result.content());
    return result;
  }
}
