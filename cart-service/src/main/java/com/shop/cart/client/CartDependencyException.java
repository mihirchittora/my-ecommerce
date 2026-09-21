package com.shop.cart.client;

import org.springframework.http.HttpStatus;

public class CartDependencyException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CartDependencyException(String dependency, String message) {
        super(dependency + " service is unavailable: " + message);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
        this.code = "DEPENDENCY_UNAVAILABLE";
    }

    public CartDependencyException(String dependency, String message, Throwable cause) {
        this(dependency, message);
        initCause(cause);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
