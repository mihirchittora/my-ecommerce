package com.shop.cart.client;

import com.shop.cart.service.CartApiException;
import org.springframework.http.HttpStatus;

public class UnknownSkuException extends CartApiException {
    public UnknownSkuException(String sku) {
        super(HttpStatus.NOT_FOUND, "SKU_NOT_FOUND", "SKU was not found in Catalog: " + sku);
    }
}
