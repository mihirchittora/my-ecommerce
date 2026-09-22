package com.shop.payment.gateway;

import java.math.BigDecimal;

public record RefundPaymentRequest(String providerPaymentId, BigDecimal amount, String currency, String reason) {
}
