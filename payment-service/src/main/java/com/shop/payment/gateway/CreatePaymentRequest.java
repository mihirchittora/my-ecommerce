package com.shop.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(UUID paymentId, UUID orderId, BigDecimal amount, String currency,
                                   String paymentMethodType) {
}
