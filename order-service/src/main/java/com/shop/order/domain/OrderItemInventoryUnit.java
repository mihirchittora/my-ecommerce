package com.shop.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "order_item_inventory_units",
        uniqueConstraints = @UniqueConstraint(name = "uq_order_item_inventory_unit",
                columnNames = {"order_item_id", "reservation_id", "inventory_unit_id"}))
@Getter
@Setter
@NoArgsConstructor
public class OrderItemInventoryUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "inventory_unit_id", nullable = false)
    private UUID inventoryUnitId;

    @Column(name = "unit_code", length = 255)
    private String unitCode;
}
