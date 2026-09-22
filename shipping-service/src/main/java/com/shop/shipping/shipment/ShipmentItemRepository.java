package com.shop.shipping.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItemEntity, UUID> {
    List<ShipmentItemEntity> findByShipment_IdOrderByCreatedAt(UUID shipmentId);
    boolean existsByInventoryUnitId(UUID inventoryUnitId);
    List<ShipmentItemEntity> findByInventoryUnitIdIn(List<UUID> inventoryUnitIds);
}
