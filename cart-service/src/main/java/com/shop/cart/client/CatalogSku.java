package com.shop.cart.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSku(
        String sku,
        UUID variantId,
        UUID productId,
        String productName,
        String variantName,
        Map<String, String> attributes,
        BigDecimal price,
        BigDecimal taxRate,
        String currency,
        boolean active) {
}
