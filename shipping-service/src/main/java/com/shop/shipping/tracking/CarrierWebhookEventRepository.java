package com.shop.shipping.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface CarrierWebhookEventRepository extends JpaRepository<CarrierWebhookEventEntity, UUID> {
    Optional<CarrierWebhookEventEntity> findByCarrierAndProviderEventId(String carrier, String providerEventId);

    /**
     * Atomically claims a carrier event before applying tracking side effects.
     * Concurrent redelivery is resolved by the database uniqueness constraint.
     */
    @Modifying
    @Transactional
    @Query(value = """
            insert into carrier_webhook_events (id, carrier, provider_event_id,
                                                provider_shipment_id, event_type,
                                                payload_hash, processed, received_at)
            values (:id, :carrier, :providerEventId,
                    :providerShipmentId, :eventType,
                    :payloadHash, false, current_timestamp)
            on conflict (carrier, provider_event_id) do nothing
            """, nativeQuery = true)
    int claim(@Param("id") UUID id,
              @Param("carrier") String carrier,
              @Param("providerEventId") String providerEventId,
              @Param("providerShipmentId") String providerShipmentId,
              @Param("eventType") String eventType,
              @Param("payloadHash") String payloadHash);
}
