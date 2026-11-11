package com.supermarket.inventory.catalog.vo;

import java.time.LocalDateTime;

public record CategoryVO(
    String id,
    String code,
    String name,
    int sortOrder,
    boolean enabled,
    long productCount,
    LocalDateTime createdAt) {}
