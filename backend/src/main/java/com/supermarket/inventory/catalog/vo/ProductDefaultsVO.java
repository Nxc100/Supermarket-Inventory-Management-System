package com.supermarket.inventory.catalog.vo;

import java.util.List;

public record ProductDefaultsVO(long stockLowerLimit, int nearExpiryDays, List<String> units) {}
