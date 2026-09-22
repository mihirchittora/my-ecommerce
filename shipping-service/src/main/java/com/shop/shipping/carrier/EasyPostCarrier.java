package com.shop.shipping.carrier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shop.shipping.common.BadRequestException;
import com.shop.shipping.common.DependencyException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * EasyPost production adapter. The API key and webhook secret are injected only
 * from runtime secrets; neither is persisted or logged.
 */
public class EasyPostCarrier implements CarrierGateway {
    private static final String WEBHOOK_PATH = "/api/v1/shipping/webhooks/EASYPOST";
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String webhookSecret;
    private final String originName;
    private final String originPhone;
    private final String originLine1;
    private final String originCity;
    private final String originState;
    private final String originPostalCode;
    private final String originCountry;
    private final double parcelLength;
    private final double parcelWidth;
    private final double parcelHeight;
    private final double parcelWeight;
    private final int timestampToleranceMinutes;

    public EasyPostCarrier(RestClient client, ObjectMapper objectMapper, String apiKey, String webhookSecret,
                           String originName, String originPhone, String originLine1, String originCity,
                           String originState, String originPostalCode, String originCountry,
                           double parcelLength, double parcelWidth, double parcelHeight, double parcelWeight,
                           int timestampToleranceMinutes) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        this.originName = originName;
        this.originPhone = originPhone;
        this.originLine1 = originLine1;
        this.originCity = originCity;
        this.originState = originState;
        this.originPostalCode = originPostalCode;
        this.originCountry = originCountry;
        this.parcelLength = parcelLength;
        this.parcelWidth = parcelWidth;
        this.parcelHeight = parcelHeight;
        this.parcelWeight = parcelWeight;
        this.timestampToleranceMinutes = timestampToleranceMinutes;
    }

    @Override
    public String name() { return "EASYPOST"; }

    @Override
    public CarrierShipmentResponse createShipment(CarrierShipmentRequest request) {
        requireCredentials();
        if (request.toAddress() == null) throw new BadRequestException("EasyPost requires an Order shipping address snapshot");
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode shipment = body.putObject("shipment");
            shipment.set("to_address", address(request.toAddress()));
            shipment.set("from_address", originAddress());
            ObjectNode parcel = shipment.putObject("parcel");
            parcel.put("length", parcelLength);
            parcel.put("width", parcelWidth);
            parcel.put("height", parcelHeight);
            parcel.put("weight", parcelWeight);
            shipment.put("reference", request.shipmentNumber());
            JsonNode created = post("/shipments", body, request.idempotencyKey());
            JsonNode shipmentNode = created.has("shipment") ? created.get("shipment") : created;
            String providerId = requiredText(shipmentNode, "id", "EasyPost did not return a shipment id");
            JsonNode rate = selectRate(shipmentNode.path("rates"), request.serviceLevel());
            ObjectNode buyBody = objectMapper.createObjectNode();
            buyBody.putObject("rate").put("id", requiredText(rate, "id", "EasyPost rate is missing an id"));
            JsonNode purchased = post("/shipments/" + providerId + "/buy", buyBody, request.idempotencyKey() + ":buy");
            JsonNode purchasedShipment = purchased.has("shipment") ? purchased.get("shipment") : purchased;
            String tracking = firstText(purchasedShipment, "tracking_code", "trackingNumber");
            if (tracking == null && purchasedShipment.has("tracker")) tracking = firstText(purchasedShipment.path("tracker"), "tracking_code");
            if (tracking == null) throw new DependencyException("EasyPost did not return a tracking code");
            String label = purchasedShipment.path("postage_label").path("label_url").asText(null);
            return new CarrierShipmentResponse(providerId, tracking, "SHIPPED", null, label);
        } catch (RestClientResponseException ex) {
            throw new DependencyException("EasyPost rejected shipment creation with HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new DependencyException("EasyPost shipment creation failed", ex);
        }
    }

    @Override
    public void cancelShipment(String providerShipmentId, String idempotencyKey) {
        requireCredentials();
        if (providerShipmentId == null || providerShipmentId.isBlank()) throw new BadRequestException("EasyPost shipment id is required");
        try {
            post("/shipments/" + providerShipmentId + "/refund", objectMapper.createObjectNode(), idempotencyKey + ":refund");
        } catch (RestClientResponseException ex) {
            throw new DependencyException("EasyPost rejected the shipment refund with HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new DependencyException("EasyPost shipment refund failed", ex);
        }
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        return verifyLegacyBodySignature(payload, signature);
    }

    @Override
    public boolean verifyWebhook(String payload, WebhookMetadata metadata) {
        if (payload == null || metadata == null || webhookSecret == null || webhookSecret.isBlank()) return false;
        String v2 = metadata.signature();
        if (v2 == null || !v2.contains("hmac-sha256-hex=")) return verifyLegacyBodySignature(payload, v2);
        if (metadata.timestamp() == null || metadata.path() == null || !WEBHOOK_PATH.equals(metadata.path())) return false;
        Instant timestamp = parseTimestamp(metadata.timestamp());
        if (timestamp == null) return false;
        Instant now = Instant.now();
        if (timestamp.isAfter(now.plusSeconds(30)) || timestamp.isBefore(now.minusSeconds(timestampToleranceMinutes * 60L))) return false;
        String expected = hmac(metadata.timestamp() + "POST" + metadata.path() + payload);
        String supplied = v2.substring(v2.indexOf("hmac-sha256-hex=") + "hmac-sha256-hex=".length());
        return MessageDigest.isEqual(expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                supplied.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public CarrierWebhookEvent parseWebhook(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"Event".equals(root.path("object").asText()) || !root.hasNonNull("id")) {
                throw new BadRequestException("EasyPost webhook is not an Event");
            }
            String description = root.path("description").asText("");
            if (!description.equals("tracker.updated") && !description.equals("shipment.updated")) {
                throw new BadRequestException("Unsupported EasyPost webhook event: " + description);
            }
            JsonNode result = root.path("result");
            String status = result.path("status").asText(null);
            String tracking = result.path("tracking_code").asText(null);
            String providerShipmentId = result.path("shipment_id").asText(null);
            if (providerShipmentId == null && "Shipment".equals(result.path("object").asText())) {
                providerShipmentId = result.path("id").asText(null);
            }
            if (status == null || (tracking == null && providerShipmentId == null)) {
                throw new BadRequestException("EasyPost webhook is missing its tracking state or shipment reference");
            }
            JsonNode detail = result.path("tracking_details");
            if (detail.isArray() && !detail.isEmpty()) detail = detail.get(detail.size() - 1);
            String location = location(detail.path("tracking_location"));
            String descriptionText = detail.path("message").asText(null);
            Instant occurred = parseInstant(detail.path("datetime").asText(null));
            if (occurred == null) occurred = parseInstant(root.path("created_at").asText(null));
            if (occurred == null) occurred = Instant.now();
            return new CarrierWebhookEvent(root.path("id").asText(), providerShipmentId, tracking,
                    normalizeEvent(status), descriptionText == null ? description : descriptionText, location, occurred);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("EasyPost webhook payload is malformed");
        }
    }

    private JsonNode post(String path, ObjectNode body, String idempotencyKey) {
        return client.post().uri(path).headers(headers -> {
            headers.setBasicAuth(apiKey, "");
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Idempotency-Key", idempotencyKey);
        }).body(body).retrieve().body(JsonNode.class);
    }

    private ObjectNode address(CarrierAddress address) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", address.recipientName());
        node.put("phone", address.phone());
        node.put("street1", address.line1());
        if (address.line2() != null) node.put("street2", address.line2());
        node.put("city", address.city());
        node.put("state", address.state());
        node.put("zip", address.postalCode());
        node.put("country", address.country());
        return node;
    }

    private ObjectNode originAddress() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", originName);
        node.put("phone", originPhone);
        node.put("street1", originLine1);
        node.put("city", originCity);
        node.put("state", originState);
        node.put("zip", originPostalCode);
        node.put("country", originCountry);
        return node;
    }

    private JsonNode selectRate(JsonNode rates, String serviceLevel) {
        if (!rates.isArray()) throw new DependencyException("EasyPost returned no shipping rates");
        for (JsonNode rate : rates) {
            if (serviceLevel.equalsIgnoreCase(rate.path("service").asText())
                    || serviceLevel.equalsIgnoreCase(rate.path("id").asText())) return rate;
        }
        throw new BadRequestException("No EasyPost rate matches serviceLevel " + serviceLevel);
    }

    private String requiredText(JsonNode node, String field, String message) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) throw new DependencyException(message);
        return value;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private void requireCredentials() {
        if (apiKey == null || apiKey.isBlank()) throw new DependencyException("EasyPost API key is not configured");
        if (isBlank(originName) || isBlank(originPhone) || isBlank(originLine1) || isBlank(originCity)
                || isBlank(originState) || isBlank(originPostalCode) || isBlank(originCountry)) {
            throw new DependencyException("EasyPost origin address is not fully configured");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private boolean verifyLegacyBodySignature(String payload, String signature) {
        if (payload == null || signature == null || signature.isBlank()
                || webhookSecret == null || webhookSecret.isBlank()) return false;
        String normalized = signature.startsWith("hmac-sha256-hex=")
                ? signature.substring("hmac-sha256-hex=".length()) : signature;
        return MessageDigest.isEqual(hmac(payload).getBytes(StandardCharsets.UTF_8), normalized.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute webhook signature", ex);
        }
    }

    private Instant parseTimestamp(String value) {
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ex) {
            try { return ZonedDateTime.parse(value, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss xx", Locale.US)).toInstant(); }
            catch (DateTimeParseException ignored) { return null; }
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value); } catch (DateTimeParseException ignored) { return null; }
    }

    private String location(JsonNode node) {
        if (node == null || node.isMissingNode()) return null;
        String city = node.path("city").asText(null);
        String state = node.path("state").asText(null);
        String country = node.path("country").asText(null);
        return java.util.stream.Stream.of(city, state, country).filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right).orElse(null);
    }

    private String normalizeEvent(String status) {
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "pre_transit", "unknown" -> "LABEL_CREATED";
            case "in_transit" -> "IN_TRANSIT";
            case "out_for_delivery" -> "OUT_FOR_DELIVERY";
            case "delivered" -> "DELIVERED";
            case "failure", "available_for_pickup" -> "DELIVERY_FAILED";
            case "return_to_sender" -> "RETURNED";
            default -> throw new BadRequestException("Unsupported EasyPost tracking status: " + status);
        };
    }
}
