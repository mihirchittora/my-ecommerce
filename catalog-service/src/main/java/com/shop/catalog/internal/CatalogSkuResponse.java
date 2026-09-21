package com.shop.catalog.internal;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.Map;

public record CatalogSkuResponse(
        String sku,
        UUID variantId,
        UUID productId,
        String productName,
        String variantName,
        Map<String, String> attributes,
        BigDecimal price,
        String currency,
        boolean active
) {
}
