package com.shop.shipping.fulfillment;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_items")
@Getter
@Setter
@NoArgsConstructor
public class FulfillmentItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fulfillment_id", nullable = false)
    private FulfillmentEntity fulfillment;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(name = "product_name_snapshot", nullable = false, length = 300)
    private String productNameSnapshot;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inventory_unit_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> inventoryUnitIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inventory_unit_codes", nullable = false, columnDefinition = "jsonb")
    private List<String> inventoryUnitCodes = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (inventoryUnitIds == null) inventoryUnitIds = new ArrayList<>();
        if (inventoryUnitCodes == null) inventoryUnitCodes = new ArrayList<>();
    }
}
