package com.shop.order.service;

import com.shop.order.api.OrderInternalDtos;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderEventType;
import com.shop.order.domain.OrderHistory;
import com.shop.order.domain.OrderRepository;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.PaymentMethod;
import com.shop.order.exception.ConflictException;
import com.shop.order.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderPaymentService {
    private final OrderRepository orders;
    private final FulfillmentOrchestrator fulfillmentOrchestrator;

    public OrderPaymentService(OrderRepository orders, FulfillmentOrchestrator fulfillmentOrchestrator) {
        this.orders = orders;
        this.fulfillmentOrchestrator = fulfillmentOrchestrator;
    }

    @Transactional
    public void applyPaymentEvent(UUID orderId, OrderInternalDtos.PaymentEventRequest request) {
        CustomerOrder order = orders.findDetailedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (!order.getCustomerId().equals(request.customerId())) {
            throw new ConflictException("Payment customer does not match the Order customer");
        }
        if (request.amount() != null && order.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new ConflictException("Payment amount does not match the Order total");
        }
        if (request.currency() != null && !order.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new ConflictException("Payment currency does not match the Order currency");
        }

        String status = request.paymentStatus().trim().toUpperCase(Locale.ROOT);
        if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                transition(order, OrderStatus.FAILED, OrderEventType.ORDER_STATE_CHANGED, request.paymentId(),
                        "Payment did not complete");
                orders.saveAndFlush(order);
            }
            return;
        }
        if (!"CAPTURED".equals(status)) return;

        if (request.paymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                || order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            if (order.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
                throw new ConflictException("COD payment does not match the Order payment method");
            }
            if (order.getStatus() == OrderStatus.DELIVERED) {
                transition(order, OrderStatus.COMPLETED, OrderEventType.ORDER_PAID, request.paymentId(),
                        "Cash on delivery collected after delivery");
                order.setCompletedAt(java.time.Instant.now());
                orders.saveAndFlush(order);
            }
            return;
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            transition(order, OrderStatus.PAID, OrderEventType.ORDER_PAID, request.paymentId(), "Payment captured");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            transition(order, OrderStatus.CONFIRMED, OrderEventType.ORDER_CONFIRMED, request.paymentId(),
                    "Order confirmed after payment capture");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.FULFILLING
                || order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED) {
            orders.saveAndFlush(order);
            fulfillmentOrchestrator.enqueue(order.getId());
        }
    }

    private void transition(CustomerOrder order, OrderStatus target, OrderEventType eventType,
                            String referenceId, String notes) {
        OrderStatus from = order.getStatus();
        OrderStateMachine.requireTransition(from, target);
        order.setStatus(target);
        if (target == OrderStatus.COMPLETED) order.setCompletedAt(java.time.Instant.now());
        OrderHistory event = new OrderHistory();
        event.setFromStatus(from);
        event.setToStatus(target);
        event.setEventType(eventType);
        event.setReferenceId(referenceId);
        event.setNotes(notes);
        event.setActorUserId("payment-service");
        order.addHistory(event);
    }
}
