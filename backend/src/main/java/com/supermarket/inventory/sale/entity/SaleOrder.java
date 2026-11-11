package com.supermarket.inventory.sale.entity;

import com.supermarket.inventory.sale.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleOrder(
    Long id,
    String orderNo,
    String requestKey,
    String requestHash,
    Long cashierId,
    String cashierName,
    SaleStatus status,
    long totalQty,
    BigDecimal totalAmount,
    PaymentMethod paymentMethod,
    BigDecimal receivedAmount,
    BigDecimal changeAmount,
    LocalDateTime completedAt,
    Long correctedBy,
    String correctorName,
    LocalDateTime correctedAt,
    String correctionReason,
    long rowVersion,
    LocalDateTime createdAt) {}
