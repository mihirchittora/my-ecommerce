package com.shop.shipping.shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "shipment_number", nullable = false, unique = true, length = 40)
    private String shipmentNumber;

    @Column(name = "fulfillment_id", nullable = false)
    private UUID fulfillmentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false, length = 200)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status;

    @Column(nullable = false, length = 50)
    private String carrier;

    @Column(name = "service_level", nullable = false, length = 50)
    private String serviceLevel;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(name = "provider_shipment_id", length = 200)
    private String providerShipmentId;

    @Column(name = "label_reference", length = 500)
    private String labelReference;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "package_count", nullable = false)
    private int packageCount = 1;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "order_notification_pending", nullable = false)
    private boolean orderNotificationPending;

    @Column(name = "order_notification_last_error", length = 1000)
    private String orderNotificationLastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (shippingCost == null) shippingCost = BigDecimal.ZERO;
        if (packageCount == 0) packageCount = 1;
        if (status == null) status = ShipmentStatus.CREATED;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
