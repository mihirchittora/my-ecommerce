package com.shop.payment;

import com.shop.payment.gateway.GatewayProvider;
import com.shop.payment.webhook.WebhookEvent;
import com.shop.payment.webhook.WebhookEventRepository;
import com.shop.payment.webhook.WebhookEventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.security.enabled=false", "server.port=0"})
@Testcontainers
class PaymentPersistenceIntegrationTest {
    static {
        PortableDockerEnvironment.configure();
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebhookEventRepository webhookEvents;

    @Test
    void webhookClaimIsAtomicAndDuplicateSafe() {
        String eventId = "evt-atomic-1";
        int first = webhookEvents.claim(UUID.randomUUID(), GatewayProvider.SANDBOX.name(), eventId,
                "payment.captured", "sandbox_payment_1", "sandbox_order_1", "hash-1");
        int duplicate = webhookEvents.claim(UUID.randomUUID(), GatewayProvider.SANDBOX.name(), eventId,
                "payment.captured", "sandbox_payment_1", "sandbox_order_1", "hash-1");

        assertThat(first).isEqualTo(1);
        assertThat(duplicate).isZero();
        WebhookEvent stored = webhookEvents.findByProviderAndProviderEventId(GatewayProvider.SANDBOX, eventId)
                .orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(WebhookEventStatus.RECEIVED);
        assertThat(stored.getPayloadHash()).isEqualTo("hash-1");
    }
}
