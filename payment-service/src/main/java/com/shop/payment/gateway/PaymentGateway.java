package com.shop.payment.gateway;

public interface PaymentGateway {
    GatewayProvider provider();

    CreatePaymentResult createPayment(CreatePaymentRequest request);

    RefundResult refundPayment(RefundPaymentRequest request);

    boolean verifyWebhook(String payload, String signature);

    GatewayWebhookEvent parseWebhookEvent(String payload);
}
