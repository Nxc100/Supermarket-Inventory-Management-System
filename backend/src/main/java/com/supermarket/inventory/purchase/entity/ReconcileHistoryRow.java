package com.supermarket.inventory.purchase.entity;

import java.time.LocalDateTime;

public record ReconcileHistoryRow(
    Long id,
    String actorName,
    String action,
    String reason,
    String beforeJson,
    String afterJson,
    LocalDateTime occurredAt) {}
