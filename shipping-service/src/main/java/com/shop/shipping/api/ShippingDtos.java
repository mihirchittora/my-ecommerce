package com.shop.shipping.api;

import com.shop.shipping.fulfillment.FulfillmentEntity;
import com.shop.shipping.fulfillment.FulfillmentHistoryEntity;
import com.shop.shipping.fulfillment.FulfillmentItemEntity;
import com.shop.shipping.fulfillment.FulfillmentStatus;
import com.shop.shipping.shipment.ShipmentEntity;
import com.shop.shipping.shipment.ShipmentHistoryEntity;
import com.shop.shipping.shipment.ShipmentItemEntity;
import com.shop.shipping.shipment.ShipmentStatus;
import com.shop.shipping.tracking.TrackingEventEntity;
import com.shop.shipping.tracking.TrackingEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ShippingDtos {
    private ShippingDtos() { }

    public record CreateFulfillmentRequest(@NotNull UUID orderId) { }

    public record ShipmentLineRequest(
            @NotNull UUID orderItemId,
            @Positive long quantity,
            @Size(max = 100) List<@NotNull UUID> inventoryUnitIds) { }

    public record CreateShipmentRequest(
            @NotNull UUID fulfillmentId,
            @NotBlank @Size(max = 50) String carrier,
            @NotBlank @Size(max = 50) String serviceLevel,
            @DecimalMin(value = "0.00") BigDecimal shippingCost,
            @Size(min = 3, max = 3) String currency,
            @Size(max = 100) List<@Valid ShipmentLineRequest> lines) { }

    public record ShipmentSummaryResponse(UUID id, String shipmentNumber, UUID fulfillmentId,
                                          UUID orderId, String orderNumber, ShipmentStatus status,
                                          String carrier, String serviceLevel, String trackingNumber,
                                          String currency, int packageCount, Instant createdAt,
                                          Instant updatedAt) {
        public static ShipmentSummaryResponse from(ShipmentEntity entity) {
            return new ShipmentSummaryResponse(entity.getId(), entity.getShipmentNumber(), entity.getFulfillmentId(),
                    entity.getOrderId(), entity.getOrderNumber(), entity.getStatus(), entity.getCarrier(),
                    entity.getServiceLevel(), entity.getTrackingNumber(), entity.getCurrency(), entity.getPackageCount(),
                    entity.getCreatedAt(), entity.getUpdatedAt());
        }
    }

    public record ShipmentResponse(UUID id, String shipmentNumber, UUID fulfillmentId, UUID orderId,
                                   String orderNumber, String customerId, ShipmentStatus status,
                                   String carrier, String serviceLevel, String trackingNumber,
                                   String providerShipmentId, String labelReference, BigDecimal shippingCost,
                                   String currency, int packageCount, Instant estimatedDeliveryAt,
                                   boolean orderNotificationPending, Instant createdAt, Instant updatedAt,
                                   Instant shippedAt, Instant deliveredAt, Instant cancelledAt,
                                   List<ShipmentItemResponse> items, List<ShipmentHistoryResponse> history) { }

    public record ShipmentItemResponse(UUID id, UUID shipmentId, UUID orderItemId, String sku,
                                       String productNameSnapshot, long quantity, UUID inventoryUnitId,
                                       Instant createdAt) {
        public static ShipmentItemResponse from(ShipmentItemEntity item) {
            return new ShipmentItemResponse(item.getId(), item.getShipment().getId(), item.getOrderItemId(),
                    item.getSku(), item.getProductNameSnapshot(), item.getQuantity(), item.getInventoryUnitId(),
                    item.getCreatedAt());
        }
    }

    public record ShipmentHistoryResponse(UUID id, ShipmentStatus fromStatus, ShipmentStatus toStatus,
                                          String eventType, String carrierEventId, String referenceId,
                                          String notes, Instant createdAt) {
        public static ShipmentHistoryResponse from(ShipmentHistoryEntity event) {
            return new ShipmentHistoryResponse(event.getId(), event.getFromStatus(), event.getToStatus(),
                    event.getEventType(), event.getCarrierEventId(), event.getReferenceId(), event.getNotes(),
                    event.getCreatedAt());
        }
    }

    public record TrackingEventResponse(UUID id, String trackingNumber, String carrier,
                                        TrackingEventType eventType, String eventStatus,
                                        String eventLocation, String description, Instant occurredAt,
                                        Instant receivedAt) {
        public static TrackingEventResponse from(TrackingEventEntity event) {
            return new TrackingEventResponse(event.getId(), event.getTrackingNumber(), event.getCarrier(),
                    event.getEventType(), event.getEventStatus(), event.getEventLocation(), event.getDescription(),
                    event.getOccurredAt(), event.getReceivedAt());
        }
    }

    public record TrackingResponse(String shipmentNumber, String carrier, String trackingNumber,
                                   ShipmentStatus currentStatus, Instant estimatedDeliveryAt,
                                   List<TrackingEventResponse> events) { }

    public record FulfillmentItemResponse(UUID id, UUID orderItemId, String sku, String productNameSnapshot,
                                          long quantity, List<UUID> inventoryUnitIds, List<String> inventoryUnitCodes,
                                          Instant createdAt) {
        public static FulfillmentItemResponse from(FulfillmentItemEntity item) {
            return new FulfillmentItemResponse(item.getId(), item.getOrderItemId(), item.getSku(),
                    item.getProductNameSnapshot(), item.getQuantity(), item.getInventoryUnitIds(),
                    item.getInventoryUnitCodes(), item.getCreatedAt());
        }
    }

    public record FulfillmentHistoryResponse(UUID id, FulfillmentStatus fromStatus, FulfillmentStatus toStatus,
                                             String eventType, String referenceId, String notes, Instant createdAt) {
        public static FulfillmentHistoryResponse from(FulfillmentHistoryEntity event) {
            return new FulfillmentHistoryResponse(event.getId(), event.getFromStatus(), event.getToStatus(),
                    event.getEventType(), event.getReferenceId(), event.getNotes(), event.getCreatedAt());
        }
    }

    public record FulfillmentResponse(UUID id, UUID orderId, String orderNumber, String customerId,
                                      FulfillmentStatus status, String shippingAddressReference,
                                      ShippingAddressResponse shippingAddress,
                                      Instant createdAt, Instant updatedAt, Instant completedAt,
                                      Instant cancelledAt, List<FulfillmentItemResponse> items,
                                      List<FulfillmentHistoryResponse> history,
                                      List<ShipmentSummaryResponse> shipments) { }

    public record ShippingAddressResponse(UUID sourceAddressId, String recipientName, String phone, String line1,
                                          String line2, String city, String state, String postalCode,
                                          String country, String landmark) { }
}
