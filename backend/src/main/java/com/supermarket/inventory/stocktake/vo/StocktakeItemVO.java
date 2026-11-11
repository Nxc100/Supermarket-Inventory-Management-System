package com.supermarket.inventory.stocktake.vo;

import com.supermarket.inventory.stocktake.enums.CountReason;
import java.time.LocalDate;

public record StocktakeItemVO(
    String id,
    String productId,
    String productCode,
    String productName,
    String unit,
    String batchItemId,
    String batchNo,
    LocalDate productionDate,
    LocalDate expiryDate,
    long bookQty,
    Long actualQty,
    Long differenceQty,
    CountReason reasonType,
    String reasonNote) {}
