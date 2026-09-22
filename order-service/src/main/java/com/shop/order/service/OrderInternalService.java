package com.shop.order.service;

import com.shop.order.api.OrderInternalDtos;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderEventType;
import com.shop.order.domain.OrderHistory;
import com.shop.order.domain.OrderRepository;
import com.shop.order.domain.OrderStatus;
import com.shop.order.exception.ConflictException;
import com.shop.order.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderInternalService {
    private final OrderRepository orders;
    private final OrderPaymentService payments;

    public OrderInternalService(OrderRepository orders, OrderPaymentService payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public OrderInternalDtos.OrderResponse get(UUID orderId) {
        CustomerOrder order = orders.findDetailedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return OrderInternalDtos.OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderInternalDtos.PaymentValidationResponse paymentValidation(UUID orderId) {
        CustomerOrder order = orders.findDetailedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return OrderInternalDtos.PaymentValidationResponse.from(order);
    }

    @Transactional
    public void shippingEvent(UUID orderId, OrderInternalDtos.ShippingEventRequest request) {
        CustomerOrder order = orders.findDetailedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        OrderStatus target;
        try { target = OrderStatus.valueOf(request.status().trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ConflictException("Unsupported Shipping order state: " + request.status()); }
        if (!Map.of(OrderStatus.FULFILLING, true, OrderStatus.SHIPPED, true, OrderStatus.DELIVERED, true).containsKey(target)) {
            throw new ConflictException("Shipping may only apply FULFILLING, SHIPPED, or DELIVERED");
        }
        if (order.getStatus() == target || isAlreadyBeyond(order.getStatus(), target)) return;
        OrderStateMachine.requireTransition(order.getStatus(), target);
        OrderStatus from = order.getStatus();
        order.setStatus(target);
        if (target == OrderStatus.DELIVERED) order.setUpdatedAt(Instant.now());
        OrderHistory event = new OrderHistory();
        event.setFromStatus(from);
        event.setToStatus(target);
        event.setEventType(OrderEventType.ORDER_STATE_CHANGED);
        event.setReferenceId(request.shipmentNumber());
        event.setNotes("Shipping Service notification");
        event.setActorUserId("shipping-service");
        order.addHistory(event);
        orders.saveAndFlush(order);
    }

    public void paymentEvent(UUID orderId, OrderInternalDtos.PaymentEventRequest request) {
        payments.applyPaymentEvent(orderId, request);
    }

    private boolean isAlreadyBeyond(OrderStatus current, OrderStatus target) {
        int currentRank = rank(current), targetRank = rank(target);
        return currentRank >= 0 && targetRank >= 0 && currentRank > targetRank;
    }

    private int rank(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> 0;
            case FULFILLING -> 1;
            case SHIPPED -> 2;
            case DELIVERED -> 3;
            case COMPLETED -> 4;
            default -> -1;
        };
    }
}
