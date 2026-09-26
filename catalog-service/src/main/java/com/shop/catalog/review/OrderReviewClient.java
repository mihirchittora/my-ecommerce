package com.shop.catalog.review;

import com.shop.catalog.common.BadRequestException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderReviewClient {
    private final RestClient client;
    public OrderReviewClient(RestClient client) { this.client = client; }
    public boolean verified(UUID orderId, UUID orderItemId, UUID productId, String sku, String customerId, String authorization) {
        try {
            OrderResponse order = client.get().uri("/api/v1/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, authorization == null ? "" : authorization).retrieve().body(OrderResponse.class);
            if (order == null || !customerId.equals(order.customerId()) || !("DELIVERED".equals(order.status()) || "COMPLETED".equals(order.status()))) return false;
            return order.items() != null && order.items().stream().anyMatch(item -> orderItemId.equals(item.id())
                    && (sku == null || sku.isBlank() || sku.equalsIgnoreCase(item.sku()))
                    && item.variantSnapshot() != null
                    && productId.toString().equals(String.valueOf(item.variantSnapshot().get("productId"))));
        } catch (RuntimeException ex) { throw new BadRequestException("Purchase verification is currently unavailable"); }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderResponse(UUID id, String customerId, String status, List<ItemResponse> items) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemResponse(UUID id, String sku, Map<String, Object> variantSnapshot) { }
}
