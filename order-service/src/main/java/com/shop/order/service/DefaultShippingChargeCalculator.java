package com.shop.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class DefaultShippingChargeCalculator implements ShippingChargeCalculator {
    private final BigDecimal freeThreshold;
    private final BigDecimal standardCharge;
    private final BigDecimal expressCharge;
    private final String freeShippingCountries;
    private final ShippingSettingsService settingsService;

    @Autowired
    public DefaultShippingChargeCalculator(
            ShippingSettingsService settingsService,
            @Value("${app.shipping.free-threshold:500}") BigDecimal freeThreshold,
            @Value("${app.shipping.standard-charge:79}") BigDecimal standardCharge,
            @Value("${app.shipping.express-charge:149}") BigDecimal expressCharge,
            @Value("${app.shipping.free-shipping-countries:IN}") String freeShippingCountries) {
        this.freeThreshold = money(freeThreshold);
        this.standardCharge = money(standardCharge);
        this.expressCharge = money(expressCharge);
        this.freeShippingCountries = freeShippingCountries == null ? "IN" : freeShippingCountries;
        this.settingsService = settingsService;
    }

    DefaultShippingChargeCalculator(BigDecimal freeThreshold, BigDecimal standardCharge,
                                    BigDecimal expressCharge, String freeShippingCountries) {
        this.freeThreshold = money(freeThreshold);
        this.standardCharge = money(standardCharge);
        this.expressCharge = money(expressCharge);
        this.freeShippingCountries = freeShippingCountries == null ? "IN" : freeShippingCountries;
        this.settingsService = null;
    }

    @Override
    public BigDecimal calculate(BigDecimal merchandiseAmount, String country, String serviceLevel) {
        BigDecimal amount = money(merchandiseAmount);
        ShippingSettingsService.ShippingSettingsValues settings = settingsService == null
                ? new ShippingSettingsService.ShippingSettingsValues(freeThreshold, standardCharge, expressCharge, freeShippingCountries)
                : settingsService.values();
        String normalizedCountry = country == null ? "" : country.trim().toUpperCase(Locale.ROOT);
        boolean freeCountry = java.util.Arrays.stream(settings.freeShippingCountries().split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedCountry::equals);
        if ("EXPRESS".equalsIgnoreCase(serviceLevel)) return settings.expressShippingCharge();
        if (freeCountry && amount.compareTo(settings.freeShippingThreshold()) > 0) return BigDecimal.ZERO.setScale(2);
        return settings.standardShippingCharge();
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("shipping values must be nonnegative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
