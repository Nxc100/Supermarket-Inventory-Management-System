package com.supermarket.inventory.system.vo;

import java.time.LocalDateTime;

public record OperationLogVO(
    String id,
    String actorName,
    String action,
    String targetType,
    String targetId,
    String targetNo,
    String result,
    String reason,
    String requestId,
    LocalDateTime occurredAt) {}
