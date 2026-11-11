package com.supermarket.inventory.ai.client;

/** Null means the provider did not report usage; it must never be presented as zero. */
public record TokenUsage(
    Long promptTokens,
    Long completionTokens,
    Long totalTokens,
    Long cachedTokens,
    String requestId) {
  public static TokenUsage unknown() {
    return new TokenUsage(null, null, null, null, null);
  }

  public boolean reported() {
    return promptTokens != null;
  }
}
