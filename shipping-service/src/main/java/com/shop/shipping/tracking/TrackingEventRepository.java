package com.shop.shipping.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEventEntity, UUID> {
    List<TrackingEventEntity> findByShipment_IdOrderByOccurredAtAsc(UUID shipmentId);
    boolean existsByCarrierAndProviderEventId(String carrier, String providerEventId);
}
