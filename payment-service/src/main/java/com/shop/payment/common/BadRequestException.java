package com.shop.payment.common;

import org.springframework.http.HttpStatus;

public class BadRequestException extends PaymentApiException {
    public BadRequestException(String message) { super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message); }
}
