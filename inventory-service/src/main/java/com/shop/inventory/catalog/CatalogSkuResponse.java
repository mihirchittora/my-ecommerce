package com.shop.inventory.catalog;

import java.util.UUID;

public record CatalogSkuResponse(String sku, UUID variantId, UUID productId, String productName, boolean active) {
    public CatalogSkuResponse(String sku, UUID variantId, UUID productId, boolean active) {
        this(sku, variantId, productId, null, active);
    }
}
