package com.shop.order.domain;

import com.shop.order.api.OrderDtos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "order_shipping_addresses")
@Getter
@NoArgsConstructor
public class OrderShippingAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true, updatable = false)
    @Setter
    private CustomerOrder order;

    @Column(name = "source_address_id", updatable = false)
    private UUID sourceAddressId;

    @Column(name = "recipient_name", nullable = false, updatable = false, length = 120)
    private String recipientName;

    @Column(nullable = false, updatable = false, length = 30)
    private String phone;

    @Column(nullable = false, updatable = false, length = 200)
    private String line1;

    @Column(updatable = false, length = 200)
    private String line2;

    @Column(nullable = false, updatable = false, length = 120)
    private String city;

    @Column(nullable = false, updatable = false, length = 120)
    private String state;

    @Column(name = "postal_code", nullable = false, updatable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, updatable = false, length = 2)
    private String country;

    @Column(updatable = false, length = 200)
    private String landmark;

    @Column(name = "snapshotted_at", nullable = false, updatable = false)
    private Instant snapshottedAt;

    public static OrderShippingAddress from(OrderDtos.ShippingAddressRequest request) {
        OrderShippingAddress snapshot = new OrderShippingAddress();
        snapshot.sourceAddressId = request.sourceAddressId();
        snapshot.recipientName = request.recipientName().trim();
        snapshot.phone = request.phone().trim();
        snapshot.line1 = request.line1().trim();
        snapshot.line2 = trimToNull(request.line2());
        snapshot.city = request.city().trim();
        snapshot.state = request.state().trim();
        snapshot.postalCode = request.postalCode().trim();
        snapshot.country = request.country().trim().toUpperCase(Locale.ROOT);
        snapshot.landmark = trimToNull(request.landmark());
        return snapshot;
    }

    @PrePersist
    void prePersist() {
        snapshottedAt = snapshottedAt == null ? Instant.now() : snapshottedAt;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
