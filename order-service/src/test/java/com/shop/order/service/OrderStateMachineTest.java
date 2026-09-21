package com.shop.order.service;

import com.shop.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineTest {
    @Test
    void acceptsOnlyExplicitBusinessTransitions() {
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING_RESERVATION, OrderStatus.RESERVED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.RESERVED, OrderStatus.PENDING_PAYMENT));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.COMPLETED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PENDING_RESERVATION, OrderStatus.COMPLETED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
    }
}
