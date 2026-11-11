package com.supermarket.inventory.system.entity;

import java.time.LocalDateTime;

/** Account-management projection deliberately excludes credentials and password hashes. */
public record StaffAccount(
    Long id,
    String username,
    String realName,
    String phone,
    String roleCode,
    boolean enabled,
    String avatarPath,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
