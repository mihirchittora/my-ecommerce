package com.shop.order.service;

import com.shop.order.api.OrderDtos;
import com.shop.order.domain.PaymentMethod;
import com.shop.order.exception.BadRequestException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OrderRequestNormalizer {
    private OrderRequestNormalizer() {
    }

    public static NormalizedRequest normalize(OrderDtos.CreateOrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("At least one order item is required");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new BadRequestException("Currency is required");
        }
        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        Map<String, Long> merged = new LinkedHashMap<>();
        for (OrderDtos.CreateOrderItemRequest line : request.items()) {
            if (line == null || line.sku() == null || line.sku().isBlank()) {
                throw new BadRequestException("Every order item requires a SKU");
            }
            if (line.quantity() <= 0) {
                throw new BadRequestException("Order item quantity must be positive");
            }
            String sku = line.sku().trim().toUpperCase(Locale.ROOT);
            try {
                merged.merge(sku, line.quantity(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Order item quantity is too large for SKU " + sku);
            }
        }
        List<NormalizedLine> lines = new ArrayList<>();
        merged.forEach((sku, quantity) -> lines.add(new NormalizedLine(sku, quantity)));
        if (request.shippingAddress() == null) {
            throw new BadRequestException("shippingAddress is required at checkout");
        }
        OrderDtos.ShippingAddressRequest address = request.shippingAddress();
        if (address.recipientName() == null || address.recipientName().isBlank()
                || address.phone() == null || address.phone().isBlank()
                || address.line1() == null || address.line1().isBlank()
                || address.city() == null || address.city().isBlank()
                || address.state() == null || address.state().isBlank()
                || address.postalCode() == null || address.postalCode().isBlank()
                || address.country() == null || !address.country().trim().matches("^[A-Za-z]{2}$")) {
            throw new BadRequestException("shippingAddress is incomplete or invalid");
        }
        OrderDtos.ShippingAddressRequest normalizedAddress = new OrderDtos.ShippingAddressRequest(
                address.sourceAddressId(), address.recipientName().trim(), address.phone().trim(), address.line1().trim(),
                blankToNull(address.line2()), address.city().trim(), address.state().trim(), address.postalCode().trim(),
                address.country().trim().toUpperCase(Locale.ROOT), blankToNull(address.landmark()));
        PaymentMethod paymentMethod = request.paymentMethod() == null ? PaymentMethod.ONLINE : request.paymentMethod();
        return new NormalizedRequest(currency, request.preferredLocationId(), normalizedAddress, lines, paymentMethod);
    }

    public record NormalizedRequest(String currency, java.util.UUID preferredLocationId,
                                    OrderDtos.ShippingAddressRequest shippingAddress,
                                    List<NormalizedLine> lines, PaymentMethod paymentMethod) {
    }

    public record NormalizedLine(String sku, long quantity) {
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
