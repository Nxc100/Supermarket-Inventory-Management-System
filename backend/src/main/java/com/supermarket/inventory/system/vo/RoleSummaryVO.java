package com.supermarket.inventory.system.vo;

import java.time.LocalDateTime;

public record RoleSummaryVO(
    String code,
    String name,
    String dataScope,
    String scopeNote,
    int permissionCount,
    long version,
    LocalDateTime updatedAt) {}
