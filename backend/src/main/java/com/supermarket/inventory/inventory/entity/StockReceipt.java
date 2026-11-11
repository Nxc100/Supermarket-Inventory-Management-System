package com.supermarket.inventory.inventory.entity;

import java.time.LocalDateTime;

public record StockReceipt(Long id, String status, LocalDateTime stockedAt) {}
