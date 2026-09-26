package com.shop.order.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommercePricingTest {
    @Test
    void appliesConfiguredShippingRulesToTheNetMerchandiseAmount() {
        var shipping = new DefaultShippingChargeCalculator(new BigDecimal("999"), new BigDecimal("79"), new BigDecimal("149"), "IN");
        assertEquals(new BigDecimal("79.00"), shipping.calculate(new BigDecimal("500"), "IN", "STANDARD"));
        assertEquals(new BigDecimal("79.00"), shipping.calculate(new BigDecimal("999"), "IN", "STANDARD"));
        assertEquals(new BigDecimal("149.00"), shipping.calculate(new BigDecimal("500"), "IN", "EXPRESS"));
    }

    @Test
    void appliesTaxOnlyToConfiguredInrOrders() {
        var tax = new DefaultTaxCalculator(new BigDecimal("18"), "IN");
        var result = tax.calculate(new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("79"), "INR", "IN");
        assertEquals(new BigDecimal("979.00"), result.taxableAmount());
        assertEquals(new BigDecimal("176.22"), result.taxAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), tax.calculate(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, "USD", "IN").taxAmount());
    }
}
