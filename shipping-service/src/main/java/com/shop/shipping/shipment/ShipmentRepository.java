package com.shop.shipping.shipment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {
    Optional<ShipmentEntity> findByShipmentNumber(String shipmentNumber);
    Optional<ShipmentEntity> findByProviderShipmentId(String providerShipmentId);
    Optional<ShipmentEntity> findByTrackingNumber(String trackingNumber);
    List<ShipmentEntity> findByFulfillmentIdOrderByCreatedAt(UUID fulfillmentId);
    Page<ShipmentEntity> findByCustomerId(String customerId, Pageable pageable);
    long countByFulfillmentIdAndStatusIn(UUID fulfillmentId, List<ShipmentStatus> statuses);
}
