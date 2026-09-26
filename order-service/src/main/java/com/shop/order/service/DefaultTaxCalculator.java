package com.shop.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.List;

/**
 * Configurable MVP tax model: one configured rate is applied to the net
 * merchandise amount plus shipping for configured countries/currencies.
 * This is not a GST compliance engine and does not infer CGST/SGST/IGST.
 */
@Component
public class DefaultTaxCalculator implements TaxCalculator {
    private final BigDecimal rate;
    private final String countries;

    public DefaultTaxCalculator(@Value("${app.tax.default-rate:18}") BigDecimal rate,
                                @Value("${app.tax.countries:IN}") String countries) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("tax rate must be between 0 and 100");
        }
        this.rate = rate.setScale(4, RoundingMode.HALF_UP);
        this.countries = countries == null ? "IN" : countries;
    }

    @Override
    public TaxResult calculate(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                               String currency, String country) {
        BigDecimal net = money(subtotal).subtract(money(discount)).max(BigDecimal.ZERO).add(money(shipping));
        boolean configured = java.util.Arrays.stream(countries.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch((country == null ? "" : country.trim().toUpperCase(Locale.ROOT))::equals);
        if (!configured || currency == null || !"INR".equalsIgnoreCase(currency)) {
            return new TaxResult(net.setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO.setScale(4), BigDecimal.ZERO.setScale(2));
        }
        BigDecimal tax = net.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new TaxResult(net.setScale(2, RoundingMode.HALF_UP), rate, tax);
    }

    @Override
    public TaxBreakdown calculate(List<TaxLine> lines, BigDecimal discount, BigDecimal shipping,
                                  String currency, String country) {
        BigDecimal subtotal = lines.stream().map(TaxLine::amount).map(this::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedDiscount = money(discount).min(subtotal);
        BigDecimal normalizedShipping = money(shipping);
        boolean configured = configured(country, currency);
        if (!configured) {
            return new TaxBreakdown(subtotal.subtract(normalizedDiscount).add(normalizedShipping)
                    .setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO.setScale(4), BigDecimal.ZERO.setScale(2),
                    lines.stream().map(line -> BigDecimal.ZERO.setScale(2)).toList(), BigDecimal.ZERO.setScale(2));
        }

        List<BigDecimal> lineTaxes = new java.util.ArrayList<>();
        BigDecimal tax = BigDecimal.ZERO;
        for (TaxLine line : lines) {
            BigDecimal lineAmount = money(line.amount());
            BigDecimal share = subtotal.signum() == 0 ? BigDecimal.ZERO : lineAmount.divide(subtotal, 8, RoundingMode.HALF_UP);
            BigDecimal lineDiscount = normalizedDiscount.multiply(share).setScale(8, RoundingMode.HALF_UP);
            BigDecimal taxable = lineAmount.subtract(lineDiscount).max(BigDecimal.ZERO);
            BigDecimal lineRate = line.taxRate() == null ? rate : line.taxRate();
            BigDecimal lineTax = taxable.multiply(lineRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            lineTaxes.add(lineTax);
            tax = tax.add(lineTax);
        }
        BigDecimal shippingTax = normalizedShipping.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        tax = tax.add(shippingTax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = subtotal.subtract(normalizedDiscount).add(normalizedShipping)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveRate = taxableAmount.signum() == 0 ? BigDecimal.ZERO
                : tax.multiply(BigDecimal.valueOf(100)).divide(taxableAmount, 4, RoundingMode.HALF_UP);
        return new TaxBreakdown(taxableAmount, effectiveRate, tax, lineTaxes, shippingTax);
    }

    private boolean configured(String country, String currency) {
        return currency != null && "INR".equalsIgnoreCase(currency)
                && java.util.Arrays.stream(countries.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch((country == null ? "" : country.trim().toUpperCase(Locale.ROOT))::equals);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("tax inputs must be nonnegative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
