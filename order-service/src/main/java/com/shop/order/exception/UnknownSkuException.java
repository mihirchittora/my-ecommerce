package com.shop.order.exception;

public class UnknownSkuException extends NotFoundException {
    public UnknownSkuException(String sku) {
        super("SKU was not found: " + sku);
    }
}
