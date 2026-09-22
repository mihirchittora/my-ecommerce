package com.shop.payment.common;

import org.springframework.http.HttpStatus;

public class GatewayException extends PaymentApiException {
    public GatewayException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "GATEWAY_ERROR", message);
        initCause(cause);
    }

    public GatewayException(String message) { super(HttpStatus.BAD_GATEWAY, "GATEWAY_ERROR", message); }
}
