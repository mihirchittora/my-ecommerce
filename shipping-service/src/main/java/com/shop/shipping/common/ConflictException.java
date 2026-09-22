package com.shop.shipping.common;

import org.springframework.http.HttpStatus;

public class ConflictException extends ShippingApiException {
    public ConflictException(String message) { super(HttpStatus.CONFLICT, "CONFLICT", message); }
}
