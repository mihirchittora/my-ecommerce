package com.shop.shipping.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_tracking_events",
        uniqueConstraints = @UniqueConstraint(name = "uq_tracking_provider_event", columnNames = {"carrier", "provider_event_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TrackingEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private com.shop.shipping.shipment.ShipmentEntity shipment;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(nullable = false, length = 50)
    private String carrier;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TrackingEventType eventType;

    @Column(name = "event_status", nullable = false, length = 50)
    private String eventStatus;

    @Column(name = "event_location", length = 200)
    private String eventLocation;

    @Column(length = 1000)
    private String description;

    @Column(name = "provider_event_id", length = 200)
    private String providerEventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (occurredAt == null) occurredAt = now;
        if (receivedAt == null) receivedAt = now;
        if (createdAt == null) createdAt = now;
    }
}
