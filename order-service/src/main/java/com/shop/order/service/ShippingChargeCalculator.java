package com.shop.order.service;

import java.math.BigDecimal;

public interface ShippingChargeCalculator {
    BigDecimal calculate(BigDecimal merchandiseAmount, String country, String serviceLevel);
}
