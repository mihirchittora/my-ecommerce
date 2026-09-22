package com.shop.shipping;

import com.shop.shipping.fulfillment.FulfillmentEntity;
import com.shop.shipping.fulfillment.FulfillmentRepository;
import com.shop.shipping.fulfillment.FulfillmentStatus;
import com.shop.shipping.tracking.CarrierWebhookEventRepository;
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
class ShippingPersistenceIntegrationTest {
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
    FulfillmentRepository fulfillments;

    @Autowired
    CarrierWebhookEventRepository webhookEvents;

    @Test
    void flywaySchemaPersistsIndependentFulfillment() {
        FulfillmentEntity entity = new FulfillmentEntity();
        entity.setOrderId(UUID.randomUUID());
        entity.setOrderNumber("ORD-20260922-000001");
        entity.setCustomerId("customer-1");
        entity.setStatus(FulfillmentStatus.READY);
        entity.setShippingAddressReference(UUID.randomUUID().toString());
        entity.setShippingRecipientName("Mihir Chittora");
        entity.setShippingPhone("+919999999999");
        entity.setShippingLine1("1 Main Street");
        entity.setShippingCity("Bengaluru");
        entity.setShippingState("Karnataka");
        entity.setShippingPostalCode("560001");
        entity.setShippingCountry("IN");
        FulfillmentEntity saved = fulfillments.saveAndFlush(entity);

        assertThat(fulfillments.findById(saved.getId())).isPresent()
                .get().extracting(FulfillmentEntity::getStatus).isEqualTo(FulfillmentStatus.READY);
        FulfillmentEntity reloaded = fulfillments.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getShippingRecipientName()).isEqualTo("Mihir Chittora");
        assertThat(reloaded.getShippingLine1()).isEqualTo("1 Main Street");
        assertThat(reloaded.getShippingCountry()).isEqualTo("IN");
    }

    @Test
    void carrierWebhookClaimIsAtomicAndDuplicateSafe() {
        String eventId = "evt-atomic-1";
        int first = webhookEvents.claim(UUID.randomUUID(), "SANDBOX", eventId,
                "sandbox_shipment_1", "IN_TRANSIT", "hash-1");
        int duplicate = webhookEvents.claim(UUID.randomUUID(), "SANDBOX", eventId,
                "sandbox_shipment_1", "IN_TRANSIT", "hash-1");

        assertThat(first).isEqualTo(1);
        assertThat(duplicate).isZero();
        assertThat(webhookEvents.findByCarrierAndProviderEventId("SANDBOX", eventId)).isPresent();
    }
}
