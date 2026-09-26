package com.shop.order.api;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderHistory;
import com.shop.order.domain.OrderItem;
import com.shop.order.domain.OrderItemInventoryUnit;
import com.shop.order.domain.OrderItemStatus;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @Schema(example = "INR")
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotEmpty @Size(max = 100) List<@Valid CreateOrderItemRequest> items,
            @Schema(description = "Optional preferred Inventory location. Inventory remains responsible for allocation.")
            UUID preferredLocationId,
            @NotNull @Valid ShippingAddressRequest shippingAddress,
            @Schema(description = "The customer's selected payment method")
            PaymentMethod paymentMethod,
            @Size(max = 40) String couponCode,
            @Size(max = 20) String serviceLevel) {
        public CreateOrderRequest(String currency, List<CreateOrderItemRequest> items, UUID preferredLocationId) {
            this(currency, items, preferredLocationId, null, PaymentMethod.ONLINE, null, "STANDARD");
        }

        public CreateOrderRequest(String currency, List<CreateOrderItemRequest> items, UUID preferredLocationId,
                                  ShippingAddressRequest shippingAddress) {
            this(currency, items, preferredLocationId, shippingAddress, PaymentMethod.ONLINE, null, "STANDARD");
        }

        public CreateOrderRequest(String currency, List<CreateOrderItemRequest> items, UUID preferredLocationId,
                                  ShippingAddressRequest shippingAddress, PaymentMethod paymentMethod) {
            this(currency, items, preferredLocationId, shippingAddress, paymentMethod, null, "STANDARD");
        }
    }

    public record CouponPreviewRequest(
            @NotBlank @Size(max = 40) String couponCode,
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal subtotal) {
    }

    public record CouponPreviewResponse(String code, BigDecimal discount, String type, BigDecimal value,
                                        BigDecimal maximumDiscount) {
    }

    public record ShippingPreviewRequest(
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal subtotal,
            @Size(max = 40) String couponCode,
            @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "^[A-Za-z]{2}$") String country) {
    }

    public record ShippingPreviewResponse(BigDecimal subtotal, BigDecimal discount, BigDecimal merchandiseAmount,
                                          List<ShippingOption> options) {
    }

    public record ShippingOption(String serviceLevel, BigDecimal amount) {
    }

    public record ShippingAddressRequest(
            UUID sourceAddressId,
            @NotBlank @Size(max = 120) String recipientName,
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[+0-9() .-]{7,30}$") String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 120) String city,
            @NotBlank @Size(max = 120) String state,
            @NotBlank @Size(max = 20)
            @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .\\-]{1,19}$") String postalCode,
            @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "^[A-Za-z]{2}$") String country,
            @Size(max = 200) String landmark) {
    }

    public record ShippingAddressResponse(
            UUID sourceAddressId,
            String recipientName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            String landmark) {
        public static ShippingAddressResponse from(com.shop.order.domain.OrderShippingAddress address) {
            return address == null ? null : new ShippingAddressResponse(address.getSourceAddressId(),
                    address.getRecipientName(), address.getPhone(), address.getLine1(), address.getLine2(),
                    address.getCity(), address.getState(), address.getPostalCode(), address.getCountry(),
                    address.getLandmark());
        }
    }

    public record CreateOrderItemRequest(
            @Schema(example = "IP17-BLK-256")
            @NotBlank @Size(max = 80) String sku,
            @Min(1) long quantity) {
    }

    public record OrderSummaryResponse(
            UUID id,
            String orderNumber,
            String customerId,
            OrderStatus status,
            PaymentMethod paymentMethod,
            String currency,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt) {
        public static OrderSummaryResponse from(CustomerOrder order) {
            return new OrderSummaryResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(),
                    order.getStatus(), order.getPaymentMethod(), order.getCurrency(), order.getTotalAmount(),
                    order.getCreatedAt(), order.getUpdatedAt());
        }
    }

    public record OrderResponse(
            UUID id,
            String orderNumber,
            String customerId,
            OrderStatus status,
            PaymentMethod paymentMethod,
            String currency,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            String couponCode,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt,
            Instant cancelledAt,
            Instant completedAt,
            ShippingAddressResponse shippingAddress,
            List<OrderItemResponse> items,
            List<OrderHistoryResponse> history) {
    }

    public record OrderItemResponse(
            UUID id,
            UUID orderId,
            String sku,
            String productNameSnapshot,
            Map<String, Object> variantSnapshot,
            BigDecimal unitPrice,
            String currency,
            long quantity,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            Instant createdAt,
            OrderItemStatus status,
            UUID reservationId,
            String reservationReference,
            List<UUID> inventoryUnitIds,
            List<InventoryUnitReference> inventoryUnits) {
    }

    public record InventoryUnitReference(
            UUID id,
            String unitCode) {
    }

    public record OrderHistoryResponse(
            UUID id,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String eventType,
            String referenceId,
            String notes,
            String actorUserId,
            Instant createdAt) {
    }

    public static OrderResponse response(CustomerOrder order, boolean operationalReferences) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> itemResponse(order, item, operationalReferences)).toList();
        List<OrderHistoryResponse> history = order.getHistory().stream()
                .map(event -> historyResponse(event, operationalReferences)).toList();
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getPaymentMethod(), order.getCurrency(), order.getSubtotal(), order.getDiscountAmount(), order.getShippingAmount(),
                order.getTaxAmount(), order.getTaxableAmount(), order.getTaxRate(), order.getCouponCode(), order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt(),
                order.getCancelledAt(), order.getCompletedAt(), ShippingAddressResponse.from(order.getShippingAddress()),
                items, history);
    }

    public record PaymentMethodOption(PaymentMethod paymentMethod, boolean eligible, String reason) { }

    public record PaymentMethodOptionsResponse(List<PaymentMethodOption> methods) { }

    private static OrderItemResponse itemResponse(CustomerOrder order, OrderItem item, boolean operationalReferences) {
        List<UUID> unitIds = operationalReferences
                ? item.getInventoryUnitReferences().stream().map(OrderItemInventoryUnit::getInventoryUnitId).toList()
                : List.of();
        List<InventoryUnitReference> units = operationalReferences
                ? item.getInventoryUnitReferences().stream()
                .map(reference -> new InventoryUnitReference(reference.getInventoryUnitId(), reference.getUnitCode()))
                .toList()
                : List.of();
        return new OrderItemResponse(item.getId(), order.getId(), item.getSku(), item.getProductNameSnapshot(),
                item.getVariantSnapshot(), item.getUnitPrice(), item.getCurrency(), item.getQuantity(),
                item.getSubtotal(), item.getDiscountAmount(), item.getTaxableAmount(), item.getTaxAmount(),
                item.getCreatedAt(), item.getStatus(), operationalReferences ? item.getReservationId() : null,
                operationalReferences ? item.getReservationReference() : null, unitIds, units);
    }

    private static OrderHistoryResponse historyResponse(OrderHistory event, boolean operationalReferences) {
        return new OrderHistoryResponse(event.getId(), event.getFromStatus(), event.getToStatus(),
                event.getEventType().name(), operationalReferences ? event.getReferenceId() : null,
                event.getNotes(), operationalReferences ? event.getActorUserId() : null, event.getCreatedAt());
    }

}
