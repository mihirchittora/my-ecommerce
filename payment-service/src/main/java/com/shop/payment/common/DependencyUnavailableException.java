package com.shop.payment.common;

import org.springframework.http.HttpStatus;

public class DependencyUnavailableException extends PaymentApiException {
    public DependencyUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", message);
        initCause(cause);
    }
}
