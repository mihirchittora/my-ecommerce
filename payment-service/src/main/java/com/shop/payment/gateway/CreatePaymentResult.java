package com.shop.payment.gateway;

import com.shop.payment.payment.PaymentStatus;

public record CreatePaymentResult(String providerPaymentId, String providerOrderId,
                                  String checkoutUrl, String checkoutToken, PaymentStatus status) {
}
