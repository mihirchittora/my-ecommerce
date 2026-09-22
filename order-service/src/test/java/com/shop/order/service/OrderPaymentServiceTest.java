package com.shop.order.service;

import com.shop.order.api.OrderInternalDtos;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderRepository;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {
    @Mock OrderRepository orders;
    @Mock FulfillmentOrchestrator fulfillmentOrchestrator;
    @InjectMocks OrderPaymentService service;

    @Test
    void capturedPaymentConfirmsOrderAndQueuesFulfillment() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = order(orderId, OrderStatus.PENDING_PAYMENT);
        when(orders.findDetailedById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentEvent(orderId, new OrderInternalDtos.PaymentEventRequest(
                "payment-1", "customer-1", "CAPTURED", new BigDecimal("125.00"), "INR", "SANDBOX", "provider-1"));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(2, order.getHistory().size());
        verify(orders).saveAndFlush(order);
        verify(fulfillmentOrchestrator).enqueue(orderId);
    }

    @Test
    void duplicateCapturedPaymentIsIdempotent() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = order(orderId, OrderStatus.CONFIRMED);
        when(orders.findDetailedById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentEvent(orderId, new OrderInternalDtos.PaymentEventRequest(
                "payment-1", "customer-1", "CAPTURED", new BigDecimal("125.00"), "INR", "SANDBOX", "provider-1"));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(fulfillmentOrchestrator).enqueue(orderId);
    }

    @Test
    void nonCapturedPaymentDoesNotConfirmOrder() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = order(orderId, OrderStatus.PENDING_PAYMENT);
        when(orders.findDetailedById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentEvent(orderId, new OrderInternalDtos.PaymentEventRequest(
                "payment-1", "customer-1", "PENDING", new BigDecimal("125.00"), "INR", "SANDBOX", "provider-1"));

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verifyNoInteractions(fulfillmentOrchestrator);
    }

    @Test
    void collectedCodPaymentCompletesAAlreadyDeliveredOrder() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = order(orderId, OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        when(orders.findDetailedById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentEvent(orderId, new OrderInternalDtos.PaymentEventRequest(
                "payment-cod-1", "customer-1", "CAPTURED", new BigDecimal("125.00"), "INR", null, null,
                PaymentMethod.CASH_ON_DELIVERY));

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(1, order.getHistory().size());
        verify(orders).saveAndFlush(order);
    }

    private CustomerOrder order(UUID id, OrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setId(id);
        order.setCustomerId("customer-1");
        order.setCurrency("INR");
        order.setTotalAmount(new BigDecimal("125.00"));
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.ONLINE);
        return order;
    }
}
