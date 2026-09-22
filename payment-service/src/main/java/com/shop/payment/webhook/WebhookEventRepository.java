package com.shop.payment.webhook;

import com.shop.payment.gateway.GatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByProviderAndProviderEventId(GatewayProvider provider, String providerEventId);
}
