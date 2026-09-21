package com.shop.order.client;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CatalogSku(
        String sku,
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        Map<String, String> attributes,
        BigDecimal price,
        String currency,
        boolean active) {
}
