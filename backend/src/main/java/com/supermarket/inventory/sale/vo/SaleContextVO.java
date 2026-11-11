package com.supermarket.inventory.sale.vo;

import com.supermarket.inventory.sale.enums.PaymentMethod;
import java.time.LocalDate;

public record SaleContextVO(LocalDate today, PaymentMethod defaultPaymentMethod) {}
