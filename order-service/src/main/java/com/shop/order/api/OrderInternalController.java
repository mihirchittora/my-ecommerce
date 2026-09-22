package com.shop.order.api;

import com.shop.order.domain.OrderStatus;
import com.shop.order.service.OrderInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Order internal integration", description = "Authenticated Shipping Service contract")
@RequestMapping("/internal/orders")
public class OrderInternalController {
    private final OrderInternalService service;

    public OrderInternalController(OrderInternalService service) { this.service = service; }

    @Operation(summary = "Read an order for fulfillment")
    @GetMapping("/{orderId}")
    public OrderInternalDtos.OrderResponse get(@PathVariable UUID orderId) { return service.get(orderId); }

    @Operation(summary = "Read payment validation data for an internal Payment operation")
    @GetMapping("/{orderId}/payment-validation")
    public OrderInternalDtos.PaymentValidationResponse paymentValidation(@PathVariable UUID orderId) {
        return service.paymentValidation(orderId);
    }

    @Operation(summary = "Apply a validated Shipping-owned milestone to the Order")
    @PostMapping("/{orderId}/shipping-events")
    public void shippingEvent(@PathVariable UUID orderId, @Valid @RequestBody OrderInternalDtos.ShippingEventRequest request) {
        service.shippingEvent(orderId, request);
    }

    @Operation(summary = "Apply a captured Payment state to the Order")
    @PostMapping("/{orderId}/payment-events")
    public void paymentEvent(@PathVariable UUID orderId,
                             @Valid @RequestBody OrderInternalDtos.PaymentEventRequest request) {
        service.paymentEvent(orderId, request);
    }
}
