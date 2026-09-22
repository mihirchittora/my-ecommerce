package com.shop.shipping.shipment;

import com.shop.shipping.carrier.CarrierGateway;
import com.shop.shipping.common.ConflictException;
import com.shop.shipping.fulfillment.FulfillmentEntity;
import com.shop.shipping.fulfillment.FulfillmentService;
import com.shop.shipping.fulfillment.FulfillmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ShipmentPersistenceService {
    private final ShipmentRepository shipments;
    private final ShipmentItemRepository items;
    private final ShipmentHistoryRepository history;
    private final ShipmentIdempotencyRepository idempotency;
    private final FulfillmentService fulfillmentService;

    public ShipmentPersistenceService(ShipmentRepository shipments, ShipmentItemRepository items,
                                      ShipmentHistoryRepository history, ShipmentIdempotencyRepository idempotency,
                                      FulfillmentService fulfillmentService) {
        this.shipments = shipments;
        this.items = items;
        this.history = history;
        this.idempotency = idempotency;
        this.fulfillmentService = fulfillmentService;
    }

    @Transactional
    public ShipmentEntity prepare(FulfillmentEntity fulfillment, String shipmentNumber, String carrier,
                                  String serviceLevel, java.math.BigDecimal shippingCost, String currency,
                                  String idempotencyKey, String requestHash, List<ShipmentService.PreparedItem> preparedItems) {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setShipmentNumber(shipmentNumber);
        shipment.setFulfillmentId(fulfillment.getId());
        shipment.setOrderId(fulfillment.getOrderId());
        shipment.setOrderNumber(fulfillment.getOrderNumber());
        shipment.setCustomerId(fulfillment.getCustomerId());
        shipment.setCarrier(carrier);
        shipment.setServiceLevel(serviceLevel);
        shipment.setShippingCost(shippingCost);
        shipment.setCurrency(currency);
        shipment.setPackageCount(1);
        shipment.setStatus(ShipmentStatus.READY);
        shipment = shipments.saveAndFlush(shipment);

        ShipmentIdempotencyEntity claim = new ShipmentIdempotencyEntity();
        claim.setFulfillment(fulfillment);
        claim.setIdempotencyKey(idempotencyKey);
        claim.setRequestHash(requestHash);
        claim.setShipment(shipment);
        idempotency.saveAndFlush(claim);

        for (ShipmentService.PreparedItem prepared : preparedItems) {
            ShipmentItemEntity item = new ShipmentItemEntity();
            item.setShipment(shipment);
            item.setOrderItemId(prepared.orderItemId());
            item.setSku(prepared.sku());
            item.setProductNameSnapshot(prepared.productNameSnapshot());
            item.setQuantity(prepared.quantity());
            item.setInventoryUnitId(prepared.inventoryUnitId());
            items.save(item);
        }
        if (fulfillment.getStatus() == FulfillmentStatus.READY || fulfillment.getStatus() == FulfillmentStatus.FAILED) {
            fulfillmentService.transition(fulfillment, FulfillmentStatus.ALLOCATING, "SHIPMENT_PREPARED",
                    "Inventory references validated; carrier creation is pending");
        }
        appendHistory(shipment, null, ShipmentStatus.READY, "SHIPMENT_CREATED", shipmentNumber,
                "Shipment persisted before carrier call");
        return shipment;
    }

    @Transactional
    public ShipmentEntity markCarrierFailure(UUID shipmentId, String message) {
        ShipmentEntity shipment = load(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.FAILED) {
            ShipmentStatus from = shipment.getStatus();
            ShipmentStateMachine.requireTransition(from, ShipmentStatus.FAILED);
            shipment.setStatus(ShipmentStatus.FAILED);
            shipments.saveAndFlush(shipment);
            appendHistory(shipment, from, ShipmentStatus.FAILED, "CARRIER_FAILED", shipment.getShipmentNumber(), message);
            FulfillmentEntity fulfillment = fulfillmentService.load(shipment.getFulfillmentId());
            if (fulfillment.getStatus() == FulfillmentStatus.ALLOCATING) {
                fulfillmentService.transition(fulfillment, FulfillmentStatus.FAILED, "CARRIER_FAILED", message);
            }
        }
        return shipment;
    }

    @Transactional
    public ShipmentEntity retryFailed(ShipmentEntity shipment) {
        if (shipment.getStatus() != ShipmentStatus.FAILED) return shipment;
        ShipmentStateMachine.requireTransition(ShipmentStatus.FAILED, ShipmentStatus.READY);
        shipment.setStatus(ShipmentStatus.READY);
        shipment.setOrderNotificationPending(false);
        shipment.setOrderNotificationLastError(null);
        shipment = shipments.saveAndFlush(shipment);
        FulfillmentEntity fulfillment = fulfillmentService.load(shipment.getFulfillmentId());
        if (fulfillment.getStatus() == FulfillmentStatus.FAILED) {
            fulfillmentService.transition(fulfillment, FulfillmentStatus.ALLOCATING, "SHIPMENT_RETRY", "Retrying carrier creation");
        }
        appendHistory(shipment, ShipmentStatus.FAILED, ShipmentStatus.READY, "SHIPMENT_RETRY",
                shipment.getShipmentNumber(), "Existing carrier idempotency record reused");
        return shipment;
    }

    @Transactional
    public ShipmentEntity markCarrierSuccess(UUID shipmentId, CarrierGateway.CarrierShipmentResponse carrier) {
        ShipmentEntity shipment = load(shipmentId);
        if (shipment.getProviderShipmentId() != null) return shipment;
        ShipmentStateMachine.requireTransition(shipment.getStatus(), ShipmentStatus.SHIPPED);
        shipment.setProviderShipmentId(carrier.providerShipmentId());
        shipment.setTrackingNumber(carrier.trackingNumber());
        shipment.setLabelReference(carrier.labelReference());
        shipment.setEstimatedDeliveryAt(carrier.estimatedDelivery());
        shipment.setShippedAt(Instant.now());
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipments.saveAndFlush(shipment);
        appendHistory(shipment, ShipmentStatus.READY, ShipmentStatus.SHIPPED, "CARRIER_CREATED",
                carrier.providerShipmentId(), "Carrier shipment created by the configured gateway");
        return shipment;
    }

    @Transactional
    public ShipmentEntity markOrderNotificationFailure(UUID shipmentId, String message) {
        ShipmentEntity shipment = load(shipmentId);
        shipment.setOrderNotificationPending(true);
        shipment.setOrderNotificationLastError(message == null ? "Order notification failed" : message.substring(0, Math.min(1000, message.length())));
        return shipments.saveAndFlush(shipment);
    }

    @Transactional
    public ShipmentEntity markOrderNotificationSuccess(UUID shipmentId) {
        ShipmentEntity shipment = load(shipmentId);
        shipment.setOrderNotificationPending(false);
        shipment.setOrderNotificationLastError(null);
        return shipments.saveAndFlush(shipment);
    }

    @Transactional
    public ShipmentEntity cancel(UUID shipmentId) {
        ShipmentEntity shipment = load(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) return shipment;
        ShipmentStateMachine.requireTransition(shipment.getStatus(), ShipmentStatus.CANCELLED);
        ShipmentStatus from = shipment.getStatus();
        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancelledAt(Instant.now());
        shipments.saveAndFlush(shipment);
        appendHistory(shipment, from, ShipmentStatus.CANCELLED, "SHIPMENT_CANCELLED", shipment.getShipmentNumber(),
                "Shipment cancelled before a non-cancellable carrier state");
        return shipment;
    }

    @Transactional
    public ShipmentEntity applyTracking(ShipmentEntity shipment, ShipmentStatus target, String eventType,
                                        String providerEventId, String notes) {
        ShipmentStatus from = shipment.getStatus();
        ShipmentStateMachine.requireTransition(from, target);
        if (from != target) {
            shipment.setStatus(target);
            if (target == ShipmentStatus.DELIVERED) shipment.setDeliveredAt(Instant.now());
            shipments.saveAndFlush(shipment);
            appendHistory(shipment, from, target, eventType, providerEventId, notes);
        }
        return shipment;
    }

    public ShipmentEntity load(UUID id) {
        return shipments.findById(id).orElseThrow(() -> new com.shop.shipping.common.NotFoundException("Shipment not found: " + id));
    }

    private void appendHistory(ShipmentEntity shipment, ShipmentStatus from, ShipmentStatus to,
                               String eventType, String referenceId, String notes) {
        ShipmentHistoryEntity event = new ShipmentHistoryEntity();
        event.setShipment(shipment);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setEventType(eventType);
        event.setReferenceId(referenceId);
        event.setNotes(notes);
        history.save(event);
    }
}
