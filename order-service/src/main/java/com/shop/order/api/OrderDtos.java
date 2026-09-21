package com.shop.order.api;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderHistory;
import com.shop.order.domain.OrderItem;
import com.shop.order.domain.OrderItemInventoryUnit;
import com.shop.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
            UUID preferredLocationId) {
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
            String currency,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt) {
        public static OrderSummaryResponse from(CustomerOrder order) {
            return new OrderSummaryResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(),
                    order.getStatus(), order.getCurrency(), order.getTotalAmount(),
                    order.getCreatedAt(), order.getUpdatedAt());
        }
    }

    public record OrderResponse(
            UUID id,
            String orderNumber,
            String customerId,
            OrderStatus status,
            String currency,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt,
            Instant cancelledAt,
            Instant completedAt,
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
            Instant createdAt,
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
                order.getCurrency(), order.getSubtotal(), order.getDiscountAmount(), order.getShippingAmount(),
                order.getTaxAmount(), order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt(),
                order.getCancelledAt(), order.getCompletedAt(), items, history);
    }

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
                item.getSubtotal(), item.getCreatedAt(), operationalReferences ? item.getReservationId() : null,
                operationalReferences ? item.getReservationReference() : null, unitIds, units);
    }

    private static OrderHistoryResponse historyResponse(OrderHistory event, boolean operationalReferences) {
        return new OrderHistoryResponse(event.getId(), event.getFromStatus(), event.getToStatus(),
                event.getEventType().name(), operationalReferences ? event.getReferenceId() : null,
                event.getNotes(), operationalReferences ? event.getActorUserId() : null, event.getCreatedAt());
    }

}
