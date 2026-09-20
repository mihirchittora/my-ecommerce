package com.shop.inventory.location;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "inventory_locations")
@Getter
@Setter
@NoArgsConstructor
public class InventoryLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        normalize();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = LocationStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        normalize();
        updatedAt = Instant.now();
    }

    private void normalize() {
        if (code != null) code = code.trim().toUpperCase(Locale.ROOT);
        if (name != null) name = name.trim();
    }
}
