package com.shop.shipping.fulfillment;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fulfillments")
@Getter
@Setter
@NoArgsConstructor
public class FulfillmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false, length = 200)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FulfillmentStatus status;

    @Column(name = "shipping_address_reference", length = 200)
    private String shippingAddressReference;

    @Column(name = "shipping_recipient_name", updatable = false, length = 120)
    private String shippingRecipientName;

    @Column(name = "shipping_phone", updatable = false, length = 30)
    private String shippingPhone;

    @Column(name = "shipping_line1", updatable = false, length = 200)
    private String shippingLine1;

    @Column(name = "shipping_line2", updatable = false, length = 200)
    private String shippingLine2;

    @Column(name = "shipping_city", updatable = false, length = 120)
    private String shippingCity;

    @Column(name = "shipping_state", updatable = false, length = 120)
    private String shippingState;

    @Column(name = "shipping_postal_code", updatable = false, length = 20)
    private String shippingPostalCode;

    @Column(name = "shipping_country", updatable = false, length = 2)
    private String shippingCountry;

    @Column(name = "shipping_landmark", updatable = false, length = 200)
    private String shippingLandmark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = FulfillmentStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
