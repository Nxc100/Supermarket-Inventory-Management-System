package com.supermarket.inventory.ai.client;

/** Only predefined safe messages cross the model boundary; provider bodies and keys never do. */
public class SuggestionFailure extends RuntimeException {
  private final String code;
  private TokenUsage usage = TokenUsage.unknown();

  public SuggestionFailure(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }

  public TokenUsage usage() {
    return usage;
  }

  public SuggestionFailure withUsage(TokenUsage usage) {
    this.usage = usage;
    return this;
  }
}
