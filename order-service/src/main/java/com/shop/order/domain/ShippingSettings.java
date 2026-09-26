package com.shop.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipping_settings")
@Getter
@Setter
@NoArgsConstructor
public class ShippingSettings {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "free_shipping_threshold", nullable = false, precision = 19, scale = 2)
    private BigDecimal freeShippingThreshold;

    @Column(name = "standard_shipping_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal standardShippingCharge;

    @Column(name = "express_shipping_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal expressShippingCharge;

    @Column(name = "free_shipping_countries", nullable = false, length = 255)
    private String freeShippingCountries;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        if (id == null) id = SINGLETON_ID;
        updatedAt = Instant.now();
    }
}
