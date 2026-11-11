package com.supermarket.inventory.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchInventory(
    Long id,
    Long purchaseId,
    Long productId,
    int quantity,
    Integer receivedQty,
    BigDecimal unitCost,
    boolean managesExpiry,
    LocalDate productionDate,
    LocalDate expiryDate,
    String batchNo,
    Long stockQty) {}
