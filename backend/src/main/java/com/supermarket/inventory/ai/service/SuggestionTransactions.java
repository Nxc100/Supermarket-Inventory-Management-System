package com.supermarket.inventory.ai.service;

import com.supermarket.inventory.ai.client.*;
import com.supermarket.inventory.ai.entity.*;
import com.supermarket.inventory.ai.mapper.SuggestionMapper;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Short database boundaries around, never across, the external model call. */
@Service
public class SuggestionTransactions {
  private final SuggestionMapper mapper;
  private final AuditService audit;
  private final Clock clock;

  public SuggestionTransactions(SuggestionMapper mapper, AuditService audit, Clock clock) {
    this.mapper = mapper;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public SuggestionRecord begin(
      PreparedSuggestion preview, CurrentUser user, SuggestionClient.Identity identity) {
    if (mapper.insertPending(preview, user, identity, LocalDateTime.now(clock)) != 1)
      throw new IllegalStateException("Suggestion insert failed");
    return mapper.byKey(preview.preview().requestKey());
  }

  @Transactional
  public SuggestionRecord finish(
      SuggestionRecord record,
      CurrentUser user,
      SuggestionClient.Result result,
      SuggestionFailure failure,
      int duration) {
    var identity =
        result == null
            ? new SuggestionClient.Identity(
                record.provider(), record.modelName(), record.isFixture())
            : result.identity();
    if (mapper.finish(
            record.id(),
            failure == null ? result.content() : null,
            failure == null ? null : failure.code(),
            failure == null ? null : failure.getMessage(),
            identity,
            failure == null ? result.usage() : failure.usage(),
            duration,
            LocalDateTime.now(clock))
        == 1)
      audit.outcome(
          user,
          failure == null ? "AI_GENERATE_SUCCESS" : "AI_GENERATE_FAILURE",
          "AI",
          record.id().toString(),
          record.requestKey(),
          failure == null,
          failure == null ? null : failure.getMessage());
    return mapper.find(record.id());
  }
}
