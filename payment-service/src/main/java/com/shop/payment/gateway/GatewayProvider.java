package com.shop.payment.gateway;

import com.shop.payment.common.BadRequestException;

import java.util.Locale;

public enum GatewayProvider {
    SANDBOX;

    public static GatewayProvider parse(String raw) {
        if (raw == null || raw.isBlank()) return SANDBOX;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported payment provider: " + raw);
        }
    }
}
