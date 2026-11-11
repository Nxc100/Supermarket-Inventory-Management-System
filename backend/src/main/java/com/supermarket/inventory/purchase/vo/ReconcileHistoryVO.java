package com.supermarket.inventory.purchase.vo;

import java.time.LocalDateTime;

public record ReconcileHistoryVO(
    String id,
    String actorName,
    String action,
    String reason,
    ReconcileSnapshotVO before,
    ReconcileSnapshotVO after,
    LocalDateTime occurredAt) {}
