package com.shop.shipping.carrier;

import com.shop.shipping.common.BadRequestException;
import com.shop.shipping.common.DependencyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class SandboxCarrier implements CarrierGateway {
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public SandboxCarrier(ObjectMapper objectMapper,
                          @Value("${app.carrier.webhook-secret:dev-shipping-webhook-secret}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String name() { return "SANDBOX"; }

    @Override
    public CarrierShipmentResponse createShipment(CarrierShipmentRequest request) {
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new DependencyException("Sandbox carrier requires an idempotency key");
        }
        String stable = sha256(request.idempotencyKey());
        return new CarrierShipmentResponse("sandbox-shipment-" + stable.substring(0, 24),
                "SBOX" + stable.substring(0, 16).toUpperCase(), "SHIPPED", null,
                "sandbox://labels/" + stable);
    }

    @Override
    public void cancelShipment(String providerShipmentId, String idempotencyKey) {
        if (providerShipmentId == null || providerShipmentId.isBlank()) {
            throw new DependencyException("Sandbox carrier shipment reference is missing");
        }
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        if (signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            String normalized = signature.startsWith("sha256=") ? signature.substring("sha256=".length()) : signature;
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), normalized.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public CarrierWebhookEvent parseWebhook(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String providerEventId = text(node, "providerEventId");
            String providerShipmentId = text(node, "providerShipmentId");
            String trackingNumber = text(node, "trackingNumber");
            String eventType = text(node, "eventType");
            if (providerEventId == null || eventType == null || (providerShipmentId == null && trackingNumber == null)) {
                throw new BadRequestException("Webhook is missing its event or shipment reference");
            }
            Instant occurredAt = node.hasNonNull("occurredAt") ? Instant.parse(node.get("occurredAt").asText()) : Instant.now();
            return new CarrierWebhookEvent(providerEventId, providerShipmentId, trackingNumber, eventType,
                    text(node, "description"), text(node, "location"), occurredAt);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Webhook payload is malformed");
        }
    }

    private String text(JsonNode node, String name) {
        return node.hasNonNull(name) && !node.get(name).asText().isBlank() ? node.get(name).asText() : null;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
