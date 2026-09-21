package com.shop.order.service;

import com.shop.order.api.OrderDtos;
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
        return new NormalizedRequest(currency, request.preferredLocationId(), lines);
    }

    public record NormalizedRequest(String currency, java.util.UUID preferredLocationId,
                                    List<NormalizedLine> lines) {
    }

    public record NormalizedLine(String sku, long quantity) {
    }
}
