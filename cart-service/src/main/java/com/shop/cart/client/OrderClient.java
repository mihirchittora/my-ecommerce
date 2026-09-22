package com.shop.cart.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderClient {
    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);
    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();
    private final RestClient client;

    public OrderClient(RestClient client) {
        this.client = client;
    }

    public OrderResponse create(CreateOrderRequest request, String idempotencyKey, String bearerToken) {
        try {
            OrderResponse response = client.post()
                    .uri("/api/v1/orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .headers(headers -> addBearer(headers, bearerToken))
                    .body(request)
                    .retrieve()
                    .body(OrderResponse.class);
            if (response == null || response.id() == null || response.orderNumber() == null) {
                throw new CartDependencyException("Order", "returned an incomplete order response");
            }
            return response;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400) throw new com.shop.cart.service.CartApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "ORDER_REJECTED",
                    upstreamMessage(ex, "Order rejected checkout inputs"));
            if (status == 409) throw new com.shop.cart.service.CartApiException(
                    org.springframework.http.HttpStatus.CONFLICT, "CHECKOUT_CONFLICT",
                    upstreamMessage(ex, "Order could not be created because inventory or checkout state conflicted"));
            if (status == 401 || status == 403) throw new CartDependencyException("Order", "rejected the authenticated checkout request", ex);
            log.warn("Order creation failed status={} idempotencyKey={}", status, idempotencyKey);
            throw new CartDependencyException("Order", "creation failed with HTTP " + status, ex);
        } catch (ResourceAccessException ex) {
            log.warn("Order creation unavailable idempotencyKey={}", idempotencyKey);
            throw new CartDependencyException("Order", "timed out or could not connect", ex);
        } catch (RestClientException ex) {
            throw new CartDependencyException("Order", "returned a malformed order response", ex);
        }
    }

    private String upstreamMessage(RestClientResponseException ex, String fallback) {
        try {
            JsonNode body = ERROR_MAPPER.readTree(ex.getResponseBodyAsString());
            String message = body.path("message").asText("").trim();
            List<String> details = new ArrayList<>();
            JsonNode detailNodes = body.path("details");
            if (detailNodes.isArray()) {
                detailNodes.forEach(detail -> {
                    String value = detail.asText("").trim();
                    if (!value.isBlank()) details.add(value);
                });
            }
            if (!details.isEmpty()) return (message.isBlank() ? fallback : message) + ": " + String.join("; ", details);
            return message.isBlank() ? fallback : message;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void addBearer(HttpHeaders headers, String bearerToken) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
    }

    public record CreateOrderRequest(String currency, List<CreateOrderItem> items, UUID preferredLocationId,
                                     ShippingAddressRequest shippingAddress, com.shop.cart.api.PaymentMethod paymentMethod) {
        public CreateOrderRequest(String currency, List<CreateOrderItem> items, UUID preferredLocationId) {
            this(currency, items, preferredLocationId, null, com.shop.cart.api.PaymentMethod.ONLINE);
        }

        public CreateOrderRequest(String currency, List<CreateOrderItem> items, UUID preferredLocationId,
                                  ShippingAddressRequest shippingAddress) {
            this(currency, items, preferredLocationId, shippingAddress, com.shop.cart.api.PaymentMethod.ONLINE);
        }
    }

    public record CreateOrderItem(String sku, long quantity) {
    }

    public record ShippingAddressRequest(UUID sourceAddressId, String recipientName, String phone, String line1,
                                         String line2, String city, String state, String postalCode, String country,
                                         String landmark) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderResponse(UUID id, String orderNumber, String status) {
    }
}
