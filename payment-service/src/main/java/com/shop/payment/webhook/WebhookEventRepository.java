package com.shop.payment.webhook;

import com.shop.payment.gateway.GatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByProviderAndProviderEventId(GatewayProvider provider, String providerEventId);

    /**
     * Atomically claims a provider event before applying its side effects.
     * The unique constraint is still the source of truth; ON CONFLICT makes
     * concurrent deliveries return a duplicate instead of raising a 500.
     */
    @Modifying
    @Transactional
    @Query(value = """
            insert into webhook_events (id, provider, provider_event_id, event_type,
                                        provider_payment_id, provider_order_id, payload_hash,
                                        status, received_at)
            values (:id, :provider, :providerEventId, :eventType,
                    :providerPaymentId, :providerOrderId, :payloadHash,
                    'RECEIVED', current_timestamp)
            on conflict (provider, provider_event_id) do nothing
            """, nativeQuery = true)
    int claim(@Param("id") UUID id,
              @Param("provider") String provider,
              @Param("providerEventId") String providerEventId,
              @Param("eventType") String eventType,
              @Param("providerPaymentId") String providerPaymentId,
              @Param("providerOrderId") String providerOrderId,
              @Param("payloadHash") String payloadHash);
}
