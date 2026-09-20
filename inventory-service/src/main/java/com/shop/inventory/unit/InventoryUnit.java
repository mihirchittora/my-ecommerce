package com.shop.inventory.unit;

import com.shop.inventory.item.InventoryItem;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "inventory_units")
@Getter
@Setter
@NoArgsConstructor
public class InventoryUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "unit_code", nullable = false, unique = true, length = 60)
    private String unitCode;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(name = "receipt_reference_id", length = 200)
    private String receiptReferenceId;

    @Column(name = "serial_number", length = 150)
    private String serialNumber;

    @Column(length = 30)
    private String imei;

    @Column(length = 150)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "sold_at")
    private Instant soldAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        normalize();
        if (unitCode == null || unitCode.isBlank()) {
            unitCode = "UNIT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        }
        Instant now = Instant.now();
        receivedAt = receivedAt == null ? now : receivedAt;
        createdAt = now;
        updatedAt = now;
        if (status == null) status = UnitStatus.AVAILABLE;
    }

    @PreUpdate
    void preUpdate() {
        normalize();
        updatedAt = Instant.now();
    }

    private void normalize() {
        if (sku != null) sku = sku.trim().toUpperCase(Locale.ROOT);
        if (receiptReferenceId != null) receiptReferenceId = receiptReferenceId.trim();
        if (serialNumber != null) serialNumber = serialNumber.trim();
        if (imei != null) imei = imei.trim();
        if (barcode != null) barcode = barcode.trim();
    }
}
