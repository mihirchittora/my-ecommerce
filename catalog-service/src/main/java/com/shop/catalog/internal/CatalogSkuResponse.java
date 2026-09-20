package com.shop.catalog.internal;

import java.util.UUID;

public record CatalogSkuResponse(
        String sku,
        UUID variantId,
        UUID productId,
        String productName,
        boolean active
) {
}
