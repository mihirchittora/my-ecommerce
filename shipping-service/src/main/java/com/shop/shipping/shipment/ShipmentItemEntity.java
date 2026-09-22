package com.shop.shipping.shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_items")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ShipmentEntity shipment;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(name = "product_name_snapshot", nullable = false, length = 300)
    private String productNameSnapshot;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "inventory_unit_id")
    private UUID inventoryUnitId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
