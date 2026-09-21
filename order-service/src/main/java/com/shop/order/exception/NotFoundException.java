package com.shop.order.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends OrderApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
