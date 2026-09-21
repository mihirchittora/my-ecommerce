package com.shop.customer.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String code, String message,
                       String path, Map<String, String> fieldErrors) {
    public ApiError(int status, String code, String message, String path) {
        this(Instant.now(), status, code, message, path, Map.of());
    }

    public ApiError(int status, String code, String message, String path, Map<String, String> fieldErrors) {
        this(Instant.now(), status, code, message, path, fieldErrors);
    }
}
