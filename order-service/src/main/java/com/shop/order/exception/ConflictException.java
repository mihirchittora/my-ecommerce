package com.shop.order.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends OrderApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
