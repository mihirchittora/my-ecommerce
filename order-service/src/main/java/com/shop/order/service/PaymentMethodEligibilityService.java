package com.shop.order.service;

import com.shop.order.api.OrderDtos;
import com.shop.order.domain.PaymentMethod;
import com.shop.order.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Service
public class PaymentMethodEligibilityService {
    private final boolean codEnabled;
    private final BigDecimal codMaxOrderAmount;
    private final Set<String> codCountries;
    private final Set<String> codCurrencies;

    public PaymentMethodEligibilityService(
            @Value("${app.cod.enabled:true}") boolean codEnabled,
            @Value("${app.cod.max-order-amount:50000}") BigDecimal codMaxOrderAmount,
            @Value("${app.cod.countries:IN}") String codCountries,
            @Value("${app.cod.currencies:INR}") String codCurrencies) {
        this.codEnabled = codEnabled;
        this.codMaxOrderAmount = codMaxOrderAmount;
        this.codCountries = values(codCountries);
        this.codCurrencies = values(codCurrencies);
    }

    public OrderDtos.PaymentMethodOptionsResponse options(String currency, BigDecimal amount, String country) {
        String normalizedCurrency = normalize(currency);
        String normalizedCountry = normalize(country);
        String reason = codReason(normalizedCurrency, amount, normalizedCountry);
        return new OrderDtos.PaymentMethodOptionsResponse(java.util.List.of(
                new OrderDtos.PaymentMethodOption(PaymentMethod.ONLINE, true, null),
                new OrderDtos.PaymentMethodOption(PaymentMethod.CASH_ON_DELIVERY, reason == null, reason)));
    }

    public void requireEligible(PaymentMethod method, String currency, BigDecimal amount, String country) {
        if (method != PaymentMethod.CASH_ON_DELIVERY) return;
        String reason = codReason(normalize(currency), amount, normalize(country));
        if (reason != null) throw new BadRequestException(reason);
    }

    private String codReason(String currency, BigDecimal amount, String country) {
        if (!codEnabled) return "Cash on delivery is not currently available";
        if (!codCurrencies.contains(currency)) return "Cash on delivery is not available for this currency";
        if (!codCountries.contains(country)) return "Cash on delivery is not available for this delivery country";
        if (amount == null || amount.signum() <= 0) return "Cash on delivery requires a positive order total";
        if (amount.compareTo(codMaxOrderAmount) > 0) {
            return "Cash on delivery is available only for orders up to " + codMaxOrderAmount.toPlainString();
        }
        return null;
    }

    private Set<String> values(String raw) {
        return Arrays.stream(raw == null ? new String[0] : raw.split(","))
                .map(this::normalize).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
