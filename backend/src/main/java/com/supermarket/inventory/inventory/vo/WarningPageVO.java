package com.supermarket.inventory.inventory.vo;

import com.supermarket.inventory.inventory.dto.WarningQuery;
import java.time.*;
import java.util.List;

public record WarningPageVO(
    List<StockWarningVO> records,
    long total,
    int pageNum,
    int pageSize,
    WarningQuery.Type type,
    LocalDate businessDate,
    LocalDateTime collectedAt) {}
