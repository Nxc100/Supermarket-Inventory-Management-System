package com.supermarket.inventory.system.service;

import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.mapper.AuditMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
  /** Callers pass explicit non-secret change projections, never account entities or requests. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void snapshot(
      CurrentUser user,
      String action,
      String type,
      String id,
      String number,
      String reason,
      Object before,
      Object after) {
    try {
      if (mapper.snapshot(
              user,
              action,
              type,
              id,
              number,
              reason,
              json.writeValueAsString(before),
              json.writeValueAsString(after),
              MDC.get("traceId"),
              LocalDateTime.now(clock))
          != 1) throw new IllegalStateException("Snapshot audit failed");
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Audit serialization failed", e);
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void outcome(
      CurrentUser user,
      String action,
      String type,
      String id,
      String number,
      boolean success,
      String reason) {
    if (mapper.outcome(
            user,
            action,
            type,
            id,
            number,
            success ? "SUCCESS" : "FAILURE",
            reason,
            MDC.get("traceId"),
            LocalDateTime.now(clock))
        != 1) throw new IllegalStateException("Outcome audit failed");
  }

  private final AuditMapper mapper;
  private final Clock clock;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public AuditService(
      AuditMapper mapper, Clock clock, com.fasterxml.jackson.databind.ObjectMapper json) {
    this.mapper = mapper;
    this.clock = clock;
    this.json = json;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void purchaseSnapshot(
      CurrentUser user,
      String action,
      String id,
      String number,
      String reason,
      Object before,
      Object after) {
    try {
      if (mapper.purchaseSnapshot(
              user,
              action,
              id,
              number,
              reason,
              json.writeValueAsString(before),
              json.writeValueAsString(after),
              MDC.get("traceId"),
              LocalDateTime.now(clock))
          != 1) throw new IllegalStateException("Snapshot audit failed");
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Audit serialization failed", e);
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      CurrentUser user, String action, String type, String id, String number, String reason) {
    if (mapper.append(
            user, action, type, id, number, reason, MDC.get("traceId"), LocalDateTime.now(clock))
        != 1) throw new IllegalStateException("Audit write failed");
  }
}
