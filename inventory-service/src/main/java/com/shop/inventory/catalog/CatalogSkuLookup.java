package com.shop.inventory.catalog;

public interface CatalogSkuLookup {
    CatalogSkuResponse requireActive(String sku);
}
