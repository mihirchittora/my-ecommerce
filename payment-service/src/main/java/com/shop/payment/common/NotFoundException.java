package com.shop.payment.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends PaymentApiException {
    public NotFoundException(String message) { super(HttpStatus.NOT_FOUND, "NOT_FOUND", message); }
}
