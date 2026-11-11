package com.supermarket.inventory.purchase.vo;

import java.time.*;

public record PurchaseContextVO(
    LocalDate today, LocalDateTime now, LocalDate summaryStartDate, LocalDate summaryEndDate) {}
