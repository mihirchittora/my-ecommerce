package com.shop.order.service;

import com.shop.order.api.ShippingSettingsDtos;
import com.shop.order.domain.ShippingSettings;
import com.shop.order.domain.ShippingSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ShippingSettingsService {
    private final ShippingSettingsRepository settingsRepository;
    private final ShippingSettingsValues environmentDefaults;

    public ShippingSettingsService(
            ShippingSettingsRepository settingsRepository,
            @Value("${app.shipping.free-threshold:500}") BigDecimal freeThreshold,
            @Value("${app.shipping.standard-charge:79}") BigDecimal standardCharge,
            @Value("${app.shipping.express-charge:149}") BigDecimal expressCharge,
            @Value("${app.shipping.free-shipping-countries:IN}") String freeShippingCountries) {
        this.settingsRepository = settingsRepository;
        this.environmentDefaults = new ShippingSettingsValues(
                money(freeThreshold), money(standardCharge), money(expressCharge), normalizeCountries(freeShippingCountries));
    }

    @Transactional(readOnly = true)
    public ShippingSettingsDtos.Response get() {
        return ShippingSettingsDtos.Response.from(settingsRepository.findById(ShippingSettings.SINGLETON_ID)
                .orElseGet(this::environmentDefaultEntity));
    }

    @Transactional(readOnly = true)
    public ShippingSettingsValues values() {
        return settingsRepository.findById(ShippingSettings.SINGLETON_ID)
                .map(settings -> new ShippingSettingsValues(settings.getFreeShippingThreshold(), settings.getStandardShippingCharge(),
                        settings.getExpressShippingCharge(), settings.getFreeShippingCountries()))
                .orElse(environmentDefaults);
    }

    @Transactional
    public ShippingSettingsDtos.Response update(ShippingSettingsDtos.UpdateRequest request) {
        ShippingSettings settings = settingsRepository.findById(ShippingSettings.SINGLETON_ID).orElseGet(ShippingSettings::new);
        settings.setId(ShippingSettings.SINGLETON_ID);
        settings.setFreeShippingThreshold(money(request.freeShippingThreshold()));
        settings.setStandardShippingCharge(money(request.standardShippingCharge()));
        settings.setExpressShippingCharge(money(request.expressShippingCharge()));
        settings.setFreeShippingCountries(normalizeCountries(request.freeShippingCountries()));
        return ShippingSettingsDtos.Response.from(settingsRepository.save(settings));
    }

    private ShippingSettings environmentDefaultEntity() {
        ShippingSettings settings = new ShippingSettings();
        settings.setId(ShippingSettings.SINGLETON_ID);
        settings.setFreeShippingThreshold(environmentDefaults.freeShippingThreshold());
        settings.setStandardShippingCharge(environmentDefaults.standardShippingCharge());
        settings.setExpressShippingCharge(environmentDefaults.expressShippingCharge());
        settings.setFreeShippingCountries(environmentDefaults.freeShippingCountries());
        return settings;
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("shipping values must be nonnegative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeCountries(String rawCountries) {
        String normalized = Arrays.stream(Objects.toString(rawCountries, "").split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        if (normalized.isBlank()) throw new IllegalArgumentException("at least one free-shipping country is required");
        return normalized;
    }

    public record ShippingSettingsValues(
            BigDecimal freeShippingThreshold,
            BigDecimal standardShippingCharge,
            BigDecimal expressShippingCharge,
            String freeShippingCountries) {
    }
}
