package com.shop.shipping.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentIdempotencyRepository extends JpaRepository<ShipmentIdempotencyEntity, UUID> {
    Optional<ShipmentIdempotencyEntity> findByFulfillment_IdAndIdempotencyKey(UUID fulfillmentId, String idempotencyKey);
}
