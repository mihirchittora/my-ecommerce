package com.shop.payment.common;

import org.springframework.http.HttpStatus;

public class ConflictException extends PaymentApiException {
    public ConflictException(String message) { super(HttpStatus.CONFLICT, "CONFLICT", message); }
}
