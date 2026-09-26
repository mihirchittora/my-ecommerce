package com.shop.order.service;

import com.shop.order.domain.OrderStatus;
import com.shop.order.exception.ConflictException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(OrderStatus.DRAFT, EnumSet.of(OrderStatus.PENDING_RESERVATION, OrderStatus.CANCELLED)),
            Map.entry(OrderStatus.PENDING_RESERVATION, EnumSet.of(OrderStatus.RESERVED, OrderStatus.FAILED, OrderStatus.CANCELLED)),
            Map.entry(OrderStatus.RESERVED, EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.CANCELLED)),
            Map.entry(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.FAILED)),
            Map.entry(OrderStatus.PAID, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)),
            Map.entry(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.FULFILLING, OrderStatus.CANCELLED)),
            Map.entry(OrderStatus.FULFILLING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED)),
            Map.entry(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED)),
            Map.entry(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.COMPLETED)),
            Map.entry(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class)));

    private OrderStateMachine() {
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return from != null && to != null && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("Order cannot transition from " + from + " to " + to);
        }
    }

    public static boolean cancellable(OrderStatus status) {
        return status == OrderStatus.DRAFT || status == OrderStatus.PENDING_RESERVATION
                || status == OrderStatus.RESERVED || status == OrderStatus.PENDING_PAYMENT;
    }

    public static boolean itemCancellable(OrderStatus status) {
        return status == OrderStatus.DRAFT || status == OrderStatus.PENDING_RESERVATION
                || status == OrderStatus.RESERVED || status == OrderStatus.PENDING_PAYMENT
                || status == OrderStatus.PAID || status == OrderStatus.CONFIRMED;
    }
}
