package com.shop.cart.api;

import com.shop.cart.client.CatalogSku;
import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartItem;
import com.shop.cart.domain.CartStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CartDtos {
    private CartDtos() {
    }

    public record AddItemRequest(
            @Schema(example = "IP17-BLK-256")
            @NotBlank @Size(max = 80) String sku,
            @Schema(example = "2", minimum = "1")
            @Min(1) @Max(100) long quantity) {
    }

    public record UpdateItemRequest(
            @Schema(example = "5", minimum = "1")
            @Min(1) @Max(100) long quantity) {
    }

    public record CheckoutRequest(
            @Schema(description = "Must match the cart currency. If omitted, the cart currency is used.", example = "INR")
            @Size(min = 3, max = 3) String currency,
            @Schema(description = "Optional preferred inventory location. Inventory allocation remains an Order concern.")
            UUID preferredLocationId) {
    }

    public record CartResponse(
            UUID id,
            String customerId,
            CartStatus status,
            String currency,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            UUID convertedOrderId,
            String convertedOrderNumber,
            long version,
            int itemCount,
            long totalQuantity,
            boolean enrichmentAvailable,
            List<String> warnings,
            List<CartItemResponse> items) {
    }

    public record CartItemResponse(
            UUID id,
            String sku,
            long quantity,
            Instant createdAt,
            Instant updatedAt,
            ProductResponse product,
            PricingResponse pricing,
            AvailabilityResponse availability,
            boolean unavailable) {
        public static CartItemResponse raw(CartItem item, String reason) {
            return new CartItemResponse(item.getId(), item.getSku(), item.getQuantity(),
                    item.getCreatedAt(), item.getUpdatedAt(), null, null,
                    new AvailabilityResponse(false, null, reason), true);
        }

        public static CartItemResponse enriched(CartItem item, CatalogSku sku) {
            return new CartItemResponse(item.getId(), item.getSku(), item.getQuantity(),
                    item.getCreatedAt(), item.getUpdatedAt(),
                    new ProductResponse(sku.productId(), sku.variantId(), sku.productName(), sku.variantName(), sku.attributes()),
                    new PricingResponse(sku.price(), sku.currency(), sku.price().multiply(BigDecimal.valueOf(item.getQuantity()))),
                    new AvailabilityResponse(false, null, "Availability is checked authoritatively by Order Service at checkout"),
                    false);
        }

        public static CartItemResponse unavailable(CartItem item, String reason) {
            return new CartItemResponse(item.getId(), item.getSku(), item.getQuantity(),
                    item.getCreatedAt(), item.getUpdatedAt(), null, null,
                    new AvailabilityResponse(false, null, reason), true);
        }
    }

    public record ProductResponse(UUID productId, UUID variantId, String name, String variant,
                                  Map<String, String> attributes) {
    }

    public record PricingResponse(BigDecimal unitPrice, String currency, BigDecimal subtotalEstimate) {
    }

    public record AvailabilityResponse(boolean known, Long availableQuantity, String message) {
    }

    public record CartSummaryResponse(
            UUID id,
            String customerId,
            CartStatus status,
            String currency,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            UUID convertedOrderId,
            String convertedOrderNumber,
            int itemCount,
            long totalQuantity) {
    }

    public record CheckoutResponse(
            UUID cartId,
            CartStatus cartStatus,
            UUID orderId,
            String orderNumber,
            String orderStatus,
            String message) {
    }
}
