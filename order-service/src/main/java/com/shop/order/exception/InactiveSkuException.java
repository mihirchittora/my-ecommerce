package com.shop.order.exception;

public class InactiveSkuException extends ConflictException {
    public InactiveSkuException(String sku) {
        super("SKU is not active or sellable: " + sku);
    }
}
