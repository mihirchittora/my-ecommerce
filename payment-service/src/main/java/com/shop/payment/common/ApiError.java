package com.shop.payment.common;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String error, String code, String message, List<String> details) {
}
