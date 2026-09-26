package com.shop.cart.api;

import com.shop.cart.client.CatalogSku;
import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartItem;
import com.shop.cart.domain.CartStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
            UUID preferredLocationId,
            @NotNull @Valid ShippingAddressRequest shippingAddress,
            PaymentMethod paymentMethod,
            @Size(max = 40) String couponCode,
            @Size(max = 20) String serviceLevel) {
        public CheckoutRequest(String currency, UUID preferredLocationId) {
            this(currency, preferredLocationId, null, PaymentMethod.ONLINE, null, "STANDARD");
        }

        public CheckoutRequest(String currency, UUID preferredLocationId, ShippingAddressRequest shippingAddress) {
            this(currency, preferredLocationId, shippingAddress, PaymentMethod.ONLINE, null, "STANDARD");
        }
    }

    public record ShippingAddressRequest(
            UUID sourceAddressId,
            @NotBlank @Size(max = 120) String recipientName,
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[+0-9() .-]{7,30}$") String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 120) String city,
            @NotBlank @Size(max = 120) String state,
            @NotBlank @Size(max = 20) @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .\\-]{1,19}$") String postalCode,
            @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "^[A-Za-z]{2}$") String country,
            @Size(max = 200) String landmark) {
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
                    CartDtos.pricing(sku, item),
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

    public record PricingResponse(BigDecimal unitPrice, String currency, BigDecimal subtotalEstimate,
                                  BigDecimal taxRate, BigDecimal taxAmount, BigDecimal unitPriceIncludingTax,
                                  BigDecimal taxAmountEstimate, BigDecimal subtotalIncludingTax) {
    }

    private static PricingResponse pricing(CatalogSku sku, CartItem item) {
        BigDecimal taxRate = sku.taxRate() == null ? BigDecimal.ZERO : sku.taxRate();
        BigDecimal unitTax = sku.price().multiply(taxRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
        BigDecimal subtotal = sku.price().multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal taxEstimate = unitTax.multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP);
        return new PricingResponse(sku.price(), sku.currency(), subtotal, taxRate, unitTax,
                sku.price().add(unitTax).setScale(2, java.math.RoundingMode.HALF_UP), taxEstimate,
                subtotal.add(taxEstimate).setScale(2, java.math.RoundingMode.HALF_UP));
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
