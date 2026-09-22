package com.shop.payment.payment;

import com.shop.payment.common.ConflictException;

import java.util.EnumSet;
import java.util.Map;

public final class PaymentStateMachine {
    private static final Map<PaymentStatus, EnumSet<PaymentStatus>> TRANSITIONS = Map.of(
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED,
                    PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.PENDING, EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED,
                    PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.AUTHORIZED, EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.PENDING_COLLECTION, EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.CANCELLED),
            PaymentStatus.CAPTURED, EnumSet.of(PaymentStatus.REFUND_PENDING, PaymentStatus.PARTIALLY_REFUNDED,
                    PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, EnumSet.of(PaymentStatus.PENDING),
            PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.REFUND_PENDING, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED,
                    PaymentStatus.CAPTURED),
            PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED),
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));

    private PaymentStateMachine() { }

    public static boolean canTransition(PaymentStatus from, PaymentStatus to) {
        return from == to || TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class)).contains(to);
    }

    public static void requireTransition(PaymentStatus from, PaymentStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("Payment cannot transition from " + from + " to " + to);
        }
    }
}
