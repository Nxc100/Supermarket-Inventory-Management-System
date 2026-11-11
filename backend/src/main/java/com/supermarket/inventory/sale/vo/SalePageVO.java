package com.supermarket.inventory.sale.vo;

import java.util.List;

public record SalePageVO(
    List<SaleSummaryVO> records, long total, int pageNum, int pageSize, SaleTotalsVO totals) {}
