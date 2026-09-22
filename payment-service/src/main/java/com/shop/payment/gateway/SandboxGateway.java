package com.shop.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.payment.common.GatewayException;
import com.shop.payment.payment.PaymentStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

public class SandboxGateway implements PaymentGateway {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public SandboxGateway(String webhookSecret, ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayProvider provider() {
        return GatewayProvider.SANDBOX;
    }

    @Override
    public CreatePaymentResult createPayment(CreatePaymentRequest request) {
        String suffix = request.paymentId().toString().replace("-", "");
        return new CreatePaymentResult("sandbox_payment_" + suffix,
                "sandbox_order_" + suffix,
                "http://localhost:3000/checkout/sandbox/" + request.paymentId(),
                "sandbox_session_" + suffix,
                PaymentStatus.PENDING);
    }

    @Override
    public RefundResult refundPayment(RefundPaymentRequest request) {
        if (request.providerPaymentId() == null || request.providerPaymentId().isBlank()) {
            return new RefundResult(null, GatewayOperationStatus.FAILED, "MISSING_PROVIDER_PAYMENT",
                    "The gateway payment reference is missing");
        }
        String material = request.providerPaymentId() + ":" + request.amount().toPlainString() + ":" + request.currency();
        String id = "sandbox_refund_" + sha256(material).substring(0, 24);
        return new RefundResult(id, GatewayOperationStatus.SUCCEEDED, null, null);
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        if (signature == null || signature.isBlank() || webhookSecret == null || webhookSecret.isBlank()) return false;
        byte[] expected = hmac(payload);
        byte[] supplied;
        try {
            supplied = Base64.getDecoder().decode(signature.trim());
        } catch (IllegalArgumentException ignored) {
            try {
                supplied = HexFormat.of().parseHex(signature.trim());
            } catch (IllegalArgumentException invalid) {
                return false;
            }
        }
        return MessageDigest.isEqual(expected, supplied);
    }

    @Override
    public GatewayWebhookEvent parseWebhookEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = text(root, "eventId");
            String eventType = text(root, "eventType");
            if (eventId == null || eventId.isBlank() || eventType == null || eventType.isBlank()) {
                throw new GatewayException("Sandbox webhook is missing eventId or eventType");
            }
            String paymentId = text(root, "providerPaymentId");
            String orderId = text(root, "providerOrderId");
            JsonNode amountNode = root.get("amount");
            BigDecimal amount = amountNode == null || amountNode.isNull() ? null : amountNode.decimalValue();
            String currency = text(root, "currency");
            String failureCode = text(root, "failureCode");
            String failureMessage = text(root, "failureMessage");
            PaymentStatus status = statusFor(eventType);
            return new GatewayWebhookEvent(eventId, eventType, paymentId, orderId, status, amount,
                    currency, failureCode, failureMessage);
        } catch (GatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GatewayException("Sandbox webhook is not valid JSON", ex);
        }
    }

    private PaymentStatus statusFor(String eventType) {
        return switch (eventType.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "payment.authorized", "authorized" -> PaymentStatus.AUTHORIZED;
            case "payment.captured", "captured", "payment.succeeded", "succeeded" -> PaymentStatus.CAPTURED;
            case "payment.failed", "failed" -> PaymentStatus.FAILED;
            case "payment.cancelled", "cancelled" -> PaymentStatus.CANCELLED;
            case "payment.refunded", "refunded" -> PaymentStatus.REFUNDED;
            case "payment.partially_refunded", "partially_refunded" -> PaymentStatus.PARTIALLY_REFUNDED;
            default -> throw new GatewayException("Unsupported sandbox webhook event type: " + eventType);
        };
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize webhook signature verification", ex);
        }
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
