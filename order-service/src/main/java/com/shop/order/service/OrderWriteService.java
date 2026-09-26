package com.shop.order.service;

import com.shop.order.client.InventoryReservation;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderEventType;
import com.shop.order.domain.OrderHistory;
import com.shop.order.domain.OrderItem;
import com.shop.order.domain.OrderItemInventoryUnit;
import com.shop.order.domain.OrderItemStatus;
import com.shop.order.domain.OrderRepository;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.PaymentMethod;
import com.shop.order.exception.ConflictException;
import com.shop.order.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderWriteService {
    private final OrderRepository orders;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderWriteService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public CustomerOrder createPending(CustomerOrder order) {
        entityManager.persist(order);
        entityManager.flush();
        return order;
    }

    @Transactional
    public void attachReservation(UUID orderId, UUID itemId, InventoryReservation reservation) {
        CustomerOrder order = load(orderId);
        if (order.getStatus() != OrderStatus.PENDING_RESERVATION) {
            throw new ConflictException("Only a pending-reservation order can receive a reservation");
        }
        OrderItem item = order.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Order item not found: " + itemId));
        if (item.getReservationId() != null && !item.getReservationId().equals(reservation.reservationId())) {
            throw new ConflictException("Order item already has a different reservation");
        }
        item.setReservationId(reservation.reservationId());
        item.setReservationReference(reservation.referenceId());
        if (item.getInventoryUnitReferences().isEmpty()) {
            reservation.units().forEach(unit -> {
                OrderItemInventoryUnit reference = new OrderItemInventoryUnit();
                reference.setReservationId(reservation.reservationId());
                reference.setInventoryUnitId(unit.unitId());
                reference.setUnitCode(unit.unitCode());
                item.addInventoryUnitReference(reference);
            });
        }
        orders.saveAndFlush(order);
    }

    @Transactional
    public void clearReservation(UUID orderId, UUID itemId) {
        CustomerOrder order = load(orderId);
        OrderItem item = order.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Order item not found: " + itemId));
        item.setReservationId(null);
        item.setReservationReference(null);
        item.getInventoryUnitReferences().clear();
        orders.saveAndFlush(order);
    }

    @Transactional
    public void recordReservationFailure(UUID orderId, String notes, String actorUserId) {
        CustomerOrder order = load(orderId);
        addHistory(order, order.getStatus(), order.getStatus(), OrderEventType.RESERVATION_FAILED,
                order.getOrderNumber(), notes, actorUserId);
        orders.saveAndFlush(order);
    }

    @Transactional
    public void markFailed(UUID orderId, String notes, String actorUserId) {
        CustomerOrder order = load(orderId);
        if (order.getStatus() == OrderStatus.FAILED) return;
        OrderStatus from = order.getStatus();
        OrderStateMachine.requireTransition(from, OrderStatus.FAILED);
        order.setStatus(OrderStatus.FAILED);
        addHistory(order, from, OrderStatus.FAILED, OrderEventType.RESERVATION_FAILED,
                order.getOrderNumber(), notes, actorUserId);
        orders.saveAndFlush(order);
    }

    @Transactional
    public void markReservedAndPendingPayment(UUID orderId, String actorUserId) {
        markReservedAndPendingPayment(orderId, actorUserId, PaymentMethod.ONLINE);
    }

    @Transactional
    public void markReservedAndPendingPayment(UUID orderId, String actorUserId, PaymentMethod paymentMethod) {
        CustomerOrder order = load(orderId);
        if (order.getStatus() != OrderStatus.PENDING_RESERVATION) return;
        if (order.getItems().stream().anyMatch(item -> item.getStatus() == OrderItemStatus.ACTIVE && item.getReservationId() == null)) {
            throw new ConflictException("Every order item must have an Inventory reservation");
        }
        OrderStatus from = order.getStatus();
        OrderStateMachine.requireTransition(from, OrderStatus.RESERVED);
        order.setStatus(OrderStatus.RESERVED);
        addHistory(order, from, OrderStatus.RESERVED, OrderEventType.RESERVATION_CONFIRMED,
                order.getOrderNumber(), "Inventory reservations confirmed", actorUserId);

        if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
            from = order.getStatus();
            OrderStateMachine.requireTransition(from, OrderStatus.CONFIRMED);
            order.setStatus(OrderStatus.CONFIRMED);
            addHistory(order, from, OrderStatus.CONFIRMED, OrderEventType.ORDER_CONFIRMED,
                    order.getOrderNumber(), "Cash on delivery order confirmed for fulfillment", actorUserId);
        } else {
            from = order.getStatus();
            OrderStateMachine.requireTransition(from, OrderStatus.PENDING_PAYMENT);
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            addHistory(order, from, OrderStatus.PENDING_PAYMENT, OrderEventType.ORDER_READY_FOR_PAYMENT,
                    order.getOrderNumber(), "Checkout is ready for a future payment service", actorUserId);
        }
        orders.saveAndFlush(order);
    }

    @Transactional
    public void cancel(UUID orderId, String actorUserId) {
        CustomerOrder order = load(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        if (!OrderStateMachine.cancellable(order.getStatus())) {
            throw new ConflictException("Order cannot be cancelled after it reaches " + order.getStatus());
        }
        OrderStatus from = order.getStatus();
        OrderStateMachine.requireTransition(from, OrderStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.getItems().forEach(item -> item.setStatus(OrderItemStatus.CANCELLED));
        addHistory(order, from, OrderStatus.CANCELLED, OrderEventType.ORDER_CANCELLED,
                order.getOrderNumber(), "Order cancelled", actorUserId);
        orders.saveAndFlush(order);
    }

    @Transactional
    public void cancelItem(UUID orderId, UUID itemId, String actorUserId) {
        CustomerOrder order = load(orderId);
        OrderItem item = order.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Order item not found: " + itemId));
        if (item.getStatus() == OrderItemStatus.CANCELLED) return;
        item.setStatus(OrderItemStatus.CANCELLED);
        item.setReservationId(null);
        item.setReservationReference(null);
        item.getInventoryUnitReferences().clear();
        addHistory(order, order.getStatus(), order.getStatus(), OrderEventType.ITEM_CANCELLED,
                item.getSku(), "Item cancelled before shipment", actorUserId);
        if (order.getItems().stream().allMatch(candidate -> candidate.getStatus() == OrderItemStatus.CANCELLED)) {
            OrderStatus from = order.getStatus();
            OrderStateMachine.requireTransition(from, OrderStatus.CANCELLED);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(Instant.now());
            addHistory(order, from, OrderStatus.CANCELLED, OrderEventType.ORDER_CANCELLED,
                    order.getOrderNumber(), "All order items cancelled", actorUserId);
        }
        orders.saveAndFlush(order);
    }

    private CustomerOrder load(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    private void addHistory(CustomerOrder order, OrderStatus from, OrderStatus to, OrderEventType eventType,
                             String referenceId, String notes, String actorUserId) {
        OrderHistory history = new OrderHistory();
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setEventType(eventType);
        history.setReferenceId(referenceId);
        history.setNotes(notes);
        history.setActorUserId(actorUserId);
        order.addHistory(history);
    }
}
