package com.shop.shipping.tracking;

import com.shop.shipping.carrier.CarrierGateway;
import com.shop.shipping.common.BadRequestException;
import com.shop.shipping.common.ConflictException;
import com.shop.shipping.common.NotFoundException;
import com.shop.shipping.fulfillment.FulfillmentEntity;
import com.shop.shipping.fulfillment.FulfillmentService;
import com.shop.shipping.fulfillment.FulfillmentStatus;
import com.shop.shipping.order.OrderClient;
import com.shop.shipping.security.SecurityAccess;
import com.shop.shipping.shipment.ShipmentEntity;
import com.shop.shipping.shipment.ShipmentPersistenceService;
import com.shop.shipping.shipment.ShipmentRepository;
import com.shop.shipping.shipment.ShipmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class TrackingService {
    private final CarrierGateway carrier;
    private final CarrierWebhookEventRepository webhookEvents;
    private final TrackingEventRepository trackingEvents;
    private final ShipmentRepository shipments;
    private final ShipmentPersistenceService persistence;
    private final FulfillmentService fulfillmentService;
    private final OrderClient orderClient;

    public TrackingService(CarrierGateway carrier, CarrierWebhookEventRepository webhookEvents,
                           TrackingEventRepository trackingEvents, ShipmentRepository shipments,
                           ShipmentPersistenceService persistence, FulfillmentService fulfillmentService,
                           OrderClient orderClient) {
        this.carrier = carrier;
        this.webhookEvents = webhookEvents;
        this.trackingEvents = trackingEvents;
        this.shipments = shipments;
        this.persistence = persistence;
        this.fulfillmentService = fulfillmentService;
        this.orderClient = orderClient;
    }

    public WebhookResult process(String carrierName, String payload, String signature) {
        return process(carrierName, payload, new CarrierGateway.WebhookMetadata(signature, null, null, "POST"));
    }

    public WebhookResult process(String carrierName, String payload, CarrierGateway.WebhookMetadata metadata) {
        String normalizedCarrier = carrierName == null ? "" : carrierName.trim().toUpperCase();
        if (!carrier.name().equals(normalizedCarrier)) throw new BadRequestException("Unsupported carrier webhook: " + normalizedCarrier);
        if (!carrier.verifyWebhook(payload, metadata)) throw new com.shop.shipping.common.ShippingApiException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature is invalid");
        CarrierGateway.CarrierWebhookEvent event = carrier.parseWebhook(payload);
        CarrierWebhookEventEntity duplicate = webhookEvents.findByCarrierAndProviderEventId(normalizedCarrier, event.providerEventId()).orElse(null);
        String payloadHash = sha256(payload);
        if (duplicate != null) return duplicateResult(event, duplicate, payloadHash);

        int claimed = webhookEvents.claim(java.util.UUID.randomUUID(), normalizedCarrier, event.providerEventId(),
                event.providerShipmentId(), event.eventType(), payloadHash);
        CarrierWebhookEventEntity received = webhookEvents.findByCarrierAndProviderEventId(normalizedCarrier,
                event.providerEventId()).orElseThrow(() -> new IllegalStateException("Webhook claim was not persisted"));
        if (claimed == 0) return duplicateResult(event, received, payloadHash);

        ShipmentEntity shipment = event.providerShipmentId() == null
                ? shipments.findByTrackingNumber(event.trackingNumber()).orElseThrow(() -> new NotFoundException("Shipment not found for tracking reference"))
                : shipments.findByProviderShipmentId(event.providerShipmentId()).orElseThrow(() -> new NotFoundException("Shipment not found for provider reference"));
        TrackingEventType eventType = normalize(event.eventType());
        ShipmentStatus target = target(eventType, shipment.getStatus());
        try {
            persistence.applyTracking(shipment, target, eventType.name(), event.providerEventId(), event.description());
        } catch (ConflictException ex) {
            received.setIgnoredReason(ex.getMessage());
            webhookEvents.saveAndFlush(received);
            return new WebhookResult(false, false, ex.getMessage());
        }

        TrackingEventEntity tracking = new TrackingEventEntity();
        tracking.setShipment(shipment);
        tracking.setTrackingNumber(shipment.getTrackingNumber());
        tracking.setCarrier(normalizedCarrier);
        tracking.setEventType(eventType);
        tracking.setEventStatus(eventType.name());
        tracking.setEventLocation(event.location());
        tracking.setDescription(event.description());
        tracking.setProviderEventId(event.providerEventId());
        tracking.setOccurredAt(event.occurredAt());
        trackingEvents.saveAndFlush(tracking);
        received.setProcessed(true);
        webhookEvents.saveAndFlush(received);

        if (eventType == TrackingEventType.DELIVERED) processDelivery(shipment);
        return new WebhookResult(false, true, null);
    }

    public ShipmentEntity manuallyDeliver(UUID shipmentId, Authentication authentication) {
        SecurityAccess.require(authentication, "SHIPPING_MANAGE");
        ShipmentEntity shipment = persistence.load(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) return shipment;

        String notes = "Marked delivered by an operator";
        persistence.applyTracking(shipment, ShipmentStatus.DELIVERED, "MANUAL_DELIVERY", "MANUAL_ADMIN", notes);

        TrackingEventEntity tracking = new TrackingEventEntity();
        tracking.setShipment(shipment);
        tracking.setTrackingNumber(shipment.getTrackingNumber());
        tracking.setCarrier(shipment.getCarrier());
        tracking.setEventType(TrackingEventType.DELIVERED);
        tracking.setEventStatus(TrackingEventType.DELIVERED.name());
        tracking.setEventLocation("Manual operator action");
        tracking.setDescription(notes);
        tracking.setOccurredAt(Instant.now());
        trackingEvents.saveAndFlush(tracking);

        processDelivery(shipment);
        return shipment;
    }

    private WebhookResult duplicateResult(CarrierGateway.CarrierWebhookEvent event,
                                           CarrierWebhookEventEntity existing,
                                           String payloadHash) {
        if (!existing.getPayloadHash().equals(payloadHash)) {
            throw new ConflictException("Provider event ID was already received with a different payload");
        }
        return new WebhookResult(true, existing.isProcessed(), existing.getIgnoredReason());
    }

    private void processDelivery(ShipmentEntity shipment) {
        FulfillmentEntity fulfillment = fulfillmentService.load(shipment.getFulfillmentId());
        List<ShipmentEntity> all = shipments.findByFulfillmentIdOrderByCreatedAt(shipment.getFulfillmentId());
        boolean allDelivered = all.stream().allMatch(item -> item.getStatus() == ShipmentStatus.DELIVERED);
        if (allDelivered && (fulfillment.getStatus() == FulfillmentStatus.SHIPPED || fulfillment.getStatus() == FulfillmentStatus.PARTIALLY_SHIPPED)) {
            fulfillmentService.transition(fulfillment, FulfillmentStatus.DELIVERED, "FULFILLMENT_DELIVERED", "All shipments delivered");
        }
        try {
            orderClient.notifyShipment(shipment.getOrderId(), shipment.getShipmentNumber(), "DELIVERED");
            persistence.markOrderNotificationSuccess(shipment.getId());
        } catch (RuntimeException ex) {
            persistence.markOrderNotificationFailure(shipment.getId(), "Order delivery notification failed: " + ex.getMessage());
        }
    }

    private TrackingEventType normalize(String event) {
        try { return TrackingEventType.valueOf(event.trim().toUpperCase()); }
        catch (Exception ex) { throw new BadRequestException("Unknown carrier event type: " + event); }
    }

    private ShipmentStatus target(TrackingEventType event, ShipmentStatus current) {
        return switch (event) {
            case SHIPMENT_CREATED, LABEL_CREATED -> current;
            case PICKED_UP -> ShipmentStatus.SHIPPED;
            case IN_TRANSIT -> ShipmentStatus.IN_TRANSIT;
            case OUT_FOR_DELIVERY -> ShipmentStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> ShipmentStatus.DELIVERED;
            case DELIVERY_FAILED -> ShipmentStatus.DELIVERY_FAILED;
            case RETURNED -> ShipmentStatus.RETURNED;
        };
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    public record WebhookResult(boolean duplicate, boolean processed, String ignoredReason) { }
}
