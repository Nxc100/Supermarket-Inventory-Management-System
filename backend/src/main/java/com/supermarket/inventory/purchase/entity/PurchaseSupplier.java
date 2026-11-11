package com.supermarket.inventory.purchase.entity;

public record PurchaseSupplier(
    Long id,
    String name,
    String contactName,
    String contactPhone,
    String address,
    boolean enabled) {}
