package com.shop.shipping.common;

import org.springframework.http.HttpStatus;

public class DependencyException extends ShippingApiException {
    public DependencyException(String message) { super(HttpStatus.BAD_GATEWAY, "DEPENDENCY_ERROR", message); }
    public DependencyException(String message, Throwable cause) { super(HttpStatus.BAD_GATEWAY, "DEPENDENCY_ERROR", message); initCause(cause); }
}
