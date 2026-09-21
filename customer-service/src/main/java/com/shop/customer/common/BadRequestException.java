package com.shop.customer.common;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) { super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message); }
}
