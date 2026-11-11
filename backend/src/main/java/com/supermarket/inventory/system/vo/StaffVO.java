package com.supermarket.inventory.system.vo;

import java.time.LocalDateTime;

public record StaffVO(
    String id,
    String username,
    String realName,
    String phone,
    String roleCode,
    String roleName,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    long unfinishedPurchases,
    long activeStocktakes,
    boolean businessReferenced) {}
