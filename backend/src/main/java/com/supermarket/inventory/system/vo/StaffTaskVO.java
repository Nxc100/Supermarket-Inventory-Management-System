package com.supermarket.inventory.system.vo;

import java.time.LocalDateTime;

public record StaffTaskVO(
    String type, String id, String orderNo, String status, LocalDateTime createdAt) {}
