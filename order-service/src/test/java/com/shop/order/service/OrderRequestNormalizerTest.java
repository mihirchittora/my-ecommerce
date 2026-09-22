package com.shop.order.service;

import com.shop.order.api.OrderDtos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderRequestNormalizerTest {
    @Test
    void mergesDuplicateSkusBeforeCatalogAndInventoryCalls() {
        var request = new OrderDtos.CreateOrderRequest("inr", List.of(
                new OrderDtos.CreateOrderItemRequest("ip17-blk-256", 1),
                new OrderDtos.CreateOrderItemRequest("IP17-BLK-256", 2)), null,
                new OrderDtos.ShippingAddressRequest(null, "Mihir Chittora", "+919999999999", "1 Main Street",
                        null, "Bengaluru", "Karnataka", "560001", "IN", null));

        var normalized = OrderRequestNormalizer.normalize(request);

        assertEquals("INR", normalized.currency());
        assertEquals(1, normalized.lines().size());
        assertEquals("IP17-BLK-256", normalized.lines().getFirst().sku());
        assertEquals(3, normalized.lines().getFirst().quantity());
    }
}
