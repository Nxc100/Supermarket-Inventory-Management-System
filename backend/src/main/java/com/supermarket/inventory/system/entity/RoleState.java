package com.supermarket.inventory.system.entity;

import java.time.LocalDateTime;

/** Persistence projection; permission JSON is decoded into a bounded public contract. */
public record RoleState(
    String code,
    String name,
    String dataScope,
    String permissionsJson,
    String defaultJson,
    String ceilingJson,
    long authVersion,
    LocalDateTime updatedAt) {}
