package com.supermarket.inventory.common;

import java.util.List;

public record PageResult<T>(List<T> records, long total, int pageNum, int pageSize) {}
