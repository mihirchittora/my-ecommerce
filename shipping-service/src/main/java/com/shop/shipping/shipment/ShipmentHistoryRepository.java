package com.shop.shipping.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentHistoryRepository extends JpaRepository<ShipmentHistoryEntity, UUID> {
    List<ShipmentHistoryEntity> findByShipment_IdOrderByCreatedAtAsc(UUID shipmentId);
}
