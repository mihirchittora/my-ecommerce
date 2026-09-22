package com.shop.shipping.carrier;

import java.time.Instant;

public interface CarrierGateway {
    String name();
    CarrierShipmentResponse createShipment(CarrierShipmentRequest request);
    void cancelShipment(String providerShipmentId, String idempotencyKey);
    boolean verifyWebhook(String payload, String signature);
    default boolean verifyWebhook(String payload, WebhookMetadata metadata) {
        return verifyWebhook(payload, metadata == null ? null : metadata.signature());
    }
    CarrierWebhookEvent parseWebhook(String payload);

    record CarrierShipmentRequest(String shipmentNumber, String serviceLevel, String currency,
                                  String idempotencyKey, int packageCount, CarrierAddress toAddress) {
        public CarrierShipmentRequest(String shipmentNumber, String serviceLevel, String currency,
                                       String idempotencyKey, int packageCount) {
            this(shipmentNumber, serviceLevel, currency, idempotencyKey, packageCount, null);
        }
    }

    record CarrierAddress(String recipientName, String phone, String line1, String line2, String city,
                          String state, String postalCode, String country) { }

    record CarrierShipmentResponse(String providerShipmentId, String trackingNumber,
                                   String status, Instant estimatedDelivery, String labelReference) { }

    record CarrierWebhookEvent(String providerEventId, String providerShipmentId,
                               String trackingNumber, String eventType, String description,
                               String location, Instant occurredAt) { }

    record WebhookMetadata(String signature, String timestamp, String path, String method) { }
}
