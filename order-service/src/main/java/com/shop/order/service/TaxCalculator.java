package com.shop.order.service;

import java.math.BigDecimal;
import java.util.List;

public interface TaxCalculator {
    TaxResult calculate(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, String currency, String country);

    default TaxBreakdown calculate(List<TaxLine> lines, BigDecimal discount, BigDecimal shipping,
                                   String currency, String country) {
        BigDecimal subtotal = lines.stream().map(TaxLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxResult result = calculate(subtotal, discount, shipping, currency, country);
        return new TaxBreakdown(result.taxableAmount(), result.taxRate(), result.taxAmount(),
                lines.stream().map(line -> BigDecimal.ZERO.setScale(2)).toList(), BigDecimal.ZERO.setScale(2));
    }

    record TaxResult(BigDecimal taxableAmount, BigDecimal taxRate, BigDecimal taxAmount) { }

    record TaxLine(BigDecimal amount, BigDecimal taxRate) { }

    record TaxBreakdown(BigDecimal taxableAmount, BigDecimal taxRate, BigDecimal taxAmount,
                        List<BigDecimal> lineTaxAmounts, BigDecimal shippingTaxAmount) { }
}
