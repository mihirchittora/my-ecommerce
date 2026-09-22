package com.shop.shipping.common;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ShippingApiException {
    public BadRequestException(String message) { super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message); }
}
