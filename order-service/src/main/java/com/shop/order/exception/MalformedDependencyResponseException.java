package com.shop.order.exception;

public class MalformedDependencyResponseException extends RemoteDependencyException {
    public MalformedDependencyResponseException(String dependency, String message) {
        super(dependency, "malformed response: " + message);
    }
}
