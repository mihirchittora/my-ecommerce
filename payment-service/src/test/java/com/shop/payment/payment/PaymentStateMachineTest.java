package com.shop.payment.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStateMachineTest {
    @Test
    void allowsTheDocumentedCaptureAndRefundLifecycle() {
        assertTrue(PaymentStateMachine.canTransition(PaymentStatus.CREATED, PaymentStatus.PENDING));
        assertTrue(PaymentStateMachine.canTransition(PaymentStatus.PENDING, PaymentStatus.CAPTURED));
        assertTrue(PaymentStateMachine.canTransition(PaymentStatus.CAPTURED, PaymentStatus.REFUND_PENDING));
        assertTrue(PaymentStateMachine.canTransition(PaymentStatus.REFUND_PENDING, PaymentStatus.PARTIALLY_REFUNDED));
        assertTrue(PaymentStateMachine.canTransition(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED));
    }

    @Test
    void rejectsOrderLikeOrBackwardsTransitions() {
        assertFalse(PaymentStateMachine.canTransition(PaymentStatus.CAPTURED, PaymentStatus.PENDING));
        assertThrows(RuntimeException.class,
                () -> PaymentStateMachine.requireTransition(PaymentStatus.REFUNDED, PaymentStatus.CAPTURED));
    }
}
