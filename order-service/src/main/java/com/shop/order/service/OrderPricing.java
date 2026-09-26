package com.shop.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class OrderPricing {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private OrderPricing() {
    }

    public static BigDecimal lineSubtotal(BigDecimal unitPrice, long quantity) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0 || quantity <= 0) {
            throw new IllegalArgumentException("unit price must be nonnegative and quantity must be positive");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public static Totals calculate(List<BigDecimal> lineSubtotals) {
        BigDecimal subtotal = lineSubtotals.stream().reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = ZERO;
        BigDecimal shipping = ZERO;
        BigDecimal tax = ZERO;
        BigDecimal total = subtotal.subtract(discount).add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);
        return new Totals(subtotal, discount, shipping, tax, total);
    }

    public static Totals calculate(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal tax) {
        BigDecimal normalizedSubtotal = money(subtotal);
        BigDecimal normalizedDiscount = money(discount).min(normalizedSubtotal);
        BigDecimal normalizedShipping = money(shipping);
        BigDecimal normalizedTax = money(tax);
        return new Totals(normalizedSubtotal, normalizedDiscount, normalizedShipping, normalizedTax,
                normalizedSubtotal.subtract(normalizedDiscount).add(normalizedShipping).add(normalizedTax)
                        .setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("money must be nonnegative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record Totals(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal shippingAmount,
                         BigDecimal taxAmount, BigDecimal totalAmount) {
    }
}
