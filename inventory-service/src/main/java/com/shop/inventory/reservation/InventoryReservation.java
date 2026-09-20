package com.shop.inventory.reservation;

import com.shop.inventory.location.InventoryLocation;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uq_inventory_reservations_reference", columnNames = "reference_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "reference_id", nullable = false, unique = true, length = 200)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationUnit> units = new ArrayList<>();

    @PrePersist
    void prePersist() {
        normalize();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ReservationStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        normalize();
        updatedAt = Instant.now();
    }

    private void normalize() {
        if (sku != null) sku = sku.trim().toUpperCase(Locale.ROOT);
        if (referenceId != null) referenceId = referenceId.trim();
    }
}
