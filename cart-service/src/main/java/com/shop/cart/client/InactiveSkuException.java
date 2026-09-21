package com.shop.cart.client;

import com.shop.cart.service.CartApiException;
import org.springframework.http.HttpStatus;

public class InactiveSkuException extends CartApiException {
    public InactiveSkuException(String sku) {
        super(HttpStatus.CONFLICT, "SKU_INACTIVE", "SKU is inactive in Catalog: " + sku);
    }
}
