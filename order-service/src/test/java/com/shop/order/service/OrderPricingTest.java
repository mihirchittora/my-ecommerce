package com.shop.order.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderPricingTest {
    @Test
    void calculatesServerSideLineAndOrderTotalsWithBigDecimal() {
        assertEquals(new BigDecimal("259800.00"),
                OrderPricing.lineSubtotal(new BigDecimal("129900.00"), 2));
        OrderPricing.Totals totals = OrderPricing.calculate(List.of(new BigDecimal("259800.00"), new BigDecimal("100.00")));
        assertEquals(new BigDecimal("259900.00"), totals.subtotal());
        assertEquals(new BigDecimal("259900.00"), totals.totalAmount());
    }
}
