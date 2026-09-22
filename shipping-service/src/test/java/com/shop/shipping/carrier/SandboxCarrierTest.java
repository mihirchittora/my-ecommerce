package com.shop.shipping.carrier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxCarrierTest {
    private final SandboxCarrier carrier = new SandboxCarrier(new ObjectMapper(), "test-secret");

    @Test
    void createsDeterministicShipmentAndVerifiesSignature() throws Exception {
        CarrierGateway.CarrierShipmentResponse first = carrier.createShipment(
                new CarrierGateway.CarrierShipmentRequest("SHP-1", "STANDARD", "INR", "same-key", 1));
        CarrierGateway.CarrierShipmentResponse second = carrier.createShipment(
                new CarrierGateway.CarrierShipmentRequest("SHP-2", "STANDARD", "INR", "same-key", 1));
        assertEquals(first.providerShipmentId(), second.providerShipmentId());

        String payload = "{\"providerEventId\":\"evt-1\",\"providerShipmentId\":\"" + first.providerShipmentId()
                + "\",\"eventType\":\"DELIVERED\"}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        assertTrue(carrier.verifyWebhook(payload, signature));
        assertFalse(carrier.verifyWebhook(payload, "bad"));
        assertEquals("evt-1", carrier.parseWebhook(payload).providerEventId());
    }
}
