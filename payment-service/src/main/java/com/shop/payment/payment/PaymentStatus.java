package com.shop.payment.payment;

public enum PaymentStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    PENDING_COLLECTION,
    FAILED,
    CANCELLED,
    REFUND_PENDING,
    PARTIALLY_REFUNDED,
    REFUNDED
}
