package com.shop.order.exception;

import org.springframework.http.HttpStatus;

public class RemoteDependencyException extends OrderApiException {
    public RemoteDependencyException(String dependency, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", dependency + " service is unavailable: " + message);
    }

    public RemoteDependencyException(String dependency, String message, Throwable cause) {
        this(dependency, message);
        initCause(cause);
    }
}
