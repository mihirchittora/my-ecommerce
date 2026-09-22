package com.shop.payment.gateway;

public record RefundResult(String providerRefundId, GatewayOperationStatus status, String failureCode,
                           String failureMessage) {
}
