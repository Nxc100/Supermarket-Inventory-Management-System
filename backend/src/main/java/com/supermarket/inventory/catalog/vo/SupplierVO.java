package com.supermarket.inventory.catalog.vo;

import java.time.LocalDateTime;

public record SupplierVO(
    String id,
    String code,
    String name,
    String contactName,
    String contactPhone,
    String address,
    boolean enabled,
    String remark,
    long purchaseCount,
    LocalDateTime createdAt) {}
