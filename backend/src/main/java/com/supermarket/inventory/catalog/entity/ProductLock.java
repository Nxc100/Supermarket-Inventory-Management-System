package com.supermarket.inventory.catalog.entity;

public record ProductLock(
    Long id,
    String code,
    Long categoryId,
    String unit,
    boolean managesExpiry,
    long stockQty,
    String imagePath) {}
