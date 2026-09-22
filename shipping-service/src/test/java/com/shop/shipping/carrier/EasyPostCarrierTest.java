package com.shop.shipping.carrier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class EasyPostCarrierTest {
    private MockRestServiceServer server;
    private EasyPostCarrier carrier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://easypost/v2");
        server = MockRestServiceServer.bindTo(builder).build();
        carrier = new EasyPostCarrier(builder.build(), new ObjectMapper(), "test-api-key", "test-webhook-secret",
                "Shop", "+15551234567", "10 Origin Road", "Austin", "TX", "78701", "US",
                10, 8, 6, 16, 1);
    }

    @AfterEach
    void verifyRequests() { server.verify(); }

    @Test
    void createsAndBuysShipmentUsingExactRateAndMapsProviderReferences() {
        server.expect(requestTo("http://easypost/v2/shipments"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"shipment":{"id":"shp_123","rates":[{"id":"rate_ground","service":"Ground"}]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://easypost/v2/shipments/shp_123/buy"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"shipment":{"id":"shp_123","tracking_code":"9400111899223856928493",
                        "postage_label":{"label_url":"https://labels.example/shp_123"}}}
                        """, MediaType.APPLICATION_JSON));

        CarrierGateway.CarrierShipmentResponse response = carrier.createShipment(
                new CarrierGateway.CarrierShipmentRequest("SHP-1", "Ground", "USD", "idem-1", 1,
                        new CarrierGateway.CarrierAddress("Jane Doe", "+15550001111", "1 Main", null,
                                "Austin", "TX", "78702", "US")));

        assertEquals("shp_123", response.providerShipmentId());
        assertEquals("9400111899223856928493", response.trackingNumber());
        assertEquals("https://labels.example/shp_123", response.labelReference());
    }

    @Test
    void verifiesEasyPostV2TimestampPathAndBodySignature() throws Exception {
        String payload = "{\"id\":\"evt_1\",\"object\":\"Event\"}";
        String timestamp = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String path = "/api/v1/shipping/webhooks/EASYPOST";
        String signature = hmac("test-webhook-secret", timestamp + "POST" + path + payload);
        CarrierGateway.WebhookMetadata metadata = new CarrierGateway.WebhookMetadata(
                "hmac-sha256-hex=" + signature, timestamp, path, "POST");

        assertTrue(carrier.verifyWebhook(payload, metadata));
        assertFalse(carrier.verifyWebhook(payload + "x", metadata));
    }

    @Test
    void parsesTrackerStatusAndUsesTrackingNumberWhenShipmentIdIsAbsent() {
        CarrierGateway.CarrierWebhookEvent event = carrier.parseWebhook("""
                {"id":"evt_2","object":"Event","description":"tracker.updated",
                 "created_at":"2026-09-22T10:00:00Z","result":{"id":"trk_1","object":"Tracker",
                 "tracking_code":"9400","status":"delivered","shipment_id":null,
                 "tracking_details":[{"message":"DELIVERED","datetime":"2026-09-22T09:59:00Z",
                 "tracking_location":{"city":"Austin","state":"TX","country":"US"}}]}}
                """);

        assertEquals("evt_2", event.providerEventId());
        assertEquals("9400", event.trackingNumber());
        assertEquals("DELIVERED", event.eventType());
        assertEquals("Austin, TX, US", event.location());
    }

    private String hmac(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
