package com.shop.shipping.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ShippingApiException {
    public NotFoundException(String message) { super(HttpStatus.NOT_FOUND, "NOT_FOUND", message); }
}
