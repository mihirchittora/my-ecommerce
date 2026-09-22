package com.shop.payment.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.payment.payment.PaymentStatus;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxGatewayTest {
    private static final String SECRET = "test-secret";

    @Test
    void createsDeterministicCheckoutReferencesWithoutCardData() {
        UUID paymentId = UUID.randomUUID();
        SandboxGateway gateway = new SandboxGateway(SECRET, new ObjectMapper());
        CreatePaymentResult result = gateway.createPayment(new CreatePaymentRequest(paymentId, UUID.randomUUID(),
                new BigDecimal("100.00"), "INR", "CARD_TOKEN"));

        assertEquals(PaymentStatus.PENDING, result.status());
        assertTrue(result.providerPaymentId().startsWith("sandbox_payment_"));
        assertTrue(result.checkoutUrl().contains(paymentId.toString()));
    }

    @Test
    void verifiesTheExactWebhookBodyAndParsesTheEvent() throws Exception {
        String payload = "{\"eventId\":\"evt-1\",\"eventType\":\"payment.captured\","
                + "\"providerPaymentId\":\"sandbox_payment_1\",\"amount\":100.00,\"currency\":\"INR\"}";
        SandboxGateway gateway = new SandboxGateway(SECRET, new ObjectMapper());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        assertTrue(gateway.verifyWebhook(payload, signature));
        assertFalse(gateway.verifyWebhook(payload + " ", signature));
        assertEquals(PaymentStatus.CAPTURED, gateway.parseWebhookEvent(payload).paymentStatus());
    }
}
