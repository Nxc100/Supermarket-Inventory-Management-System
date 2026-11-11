package com.supermarket.inventory.inventory.vo;

import com.supermarket.inventory.inventory.enums.MovementType;
import java.time.LocalDateTime;

public record StockMovementVO(
    String id,
    String productId,
    String productCode,
    String productName,
    String unit,
    String batchId,
    String batchNo,
    MovementType eventType,
    String businessNo,
    long deltaQty,
    long batchBefore,
    long batchAfter,
    long productBefore,
    long productAfter,
    String actorName,
    String reason,
    LocalDateTime occurredAt,
    String purchaseId,
    boolean sourceReadable) {}
