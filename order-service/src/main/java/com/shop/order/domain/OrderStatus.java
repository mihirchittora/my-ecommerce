package com.shop.order.domain;

public enum OrderStatus {
    DRAFT,
    PENDING_RESERVATION,
    RESERVED,
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    FULFILLING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    FAILED
}
