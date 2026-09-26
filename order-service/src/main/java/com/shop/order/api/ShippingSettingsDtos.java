package com.shop.order.api;

import com.shop.order.domain.ShippingSettings;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class ShippingSettingsDtos {
    private ShippingSettingsDtos() { }

    public record UpdateRequest(
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal freeShippingThreshold,
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal standardShippingCharge,
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal expressShippingCharge,
            @NotBlank @Size(max = 255) String freeShippingCountries) {
    }

    public record Response(
            BigDecimal freeShippingThreshold,
            BigDecimal standardShippingCharge,
            BigDecimal expressShippingCharge,
            String freeShippingCountries,
            Instant updatedAt) {
        public static Response from(ShippingSettings settings) {
            return new Response(settings.getFreeShippingThreshold(), settings.getStandardShippingCharge(),
                    settings.getExpressShippingCharge(), settings.getFreeShippingCountries(), settings.getUpdatedAt());
        }
    }
}
