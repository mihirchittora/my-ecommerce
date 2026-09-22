package com.shop.shipping.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carrier_webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uq_carrier_webhook_event", columnNames = {"carrier", "provider_event_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CarrierWebhookEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String carrier;

    @Column(name = "provider_event_id", nullable = false, length = 200)
    private String providerEventId;

    @Column(name = "provider_shipment_id", length = 200)
    private String providerShipmentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "ignored_reason", length = 500)
    private String ignoredReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    void prePersist() { if (receivedAt == null) receivedAt = Instant.now(); }
}
