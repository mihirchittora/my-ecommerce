package com.shop.payment.gateway;

import com.shop.payment.payment.PaymentStatus;

import java.math.BigDecimal;

public record GatewayWebhookEvent(String providerEventId, String eventType, String providerPaymentId,
                                  String providerOrderId, PaymentStatus paymentStatus, BigDecimal amount,
                                  String currency, String failureCode, String failureMessage) {
}
