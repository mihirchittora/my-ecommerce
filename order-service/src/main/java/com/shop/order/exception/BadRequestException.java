package com.shop.order.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends OrderApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
