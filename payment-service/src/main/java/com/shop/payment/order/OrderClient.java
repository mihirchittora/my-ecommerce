package com.shop.payment.order;

import com.shop.payment.common.DependencyUnavailableException;
import com.shop.payment.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

public class OrderClient {
    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);
    private final RestClient client;
    private final String serviceToken;

    public OrderClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public OrderSnapshot getOrder(UUID orderId, String authorizationHeader) {
        try {
            OrderSnapshot response = client.get().uri("/api/v1/orders/{orderId}", orderId)
                    .headers(headers -> addHeaders(headers, authorizationHeader))
                    .retrieve().body(OrderSnapshot.class);
            if (response == null || response.id() == null || response.customerId() == null
                    || response.status() == null || response.currency() == null || response.totalAmount() == null) {
                throw new DependencyUnavailableException("Order Service returned an incomplete order", null);
            }
            if (!orderId.equals(response.id())) {
                throw new DependencyUnavailableException("Order Service returned a different order", null);
            }
            return response;
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 404) throw new NotFoundException("Order not found: " + orderId);
            log.warn("Order lookup failed for orderId={} status={}", orderId, status.value());
            throw new DependencyUnavailableException("Order Service lookup failed", ex);
        } catch (ResourceAccessException ex) {
            log.warn("Order Service unavailable for orderId={}", orderId);
            throw new DependencyUnavailableException("Order Service timed out or could not connect", ex);
        } catch (RestClientException ex) {
            throw new DependencyUnavailableException("Order Service returned a malformed response", ex);
        }
    }

    public OrderSnapshot getPaymentValidation(UUID orderId) {
        try {
            OrderSnapshot response = client.get().uri("/internal/orders/{orderId}/payment-validation", orderId)
                    .headers(headers -> {
                        if (serviceToken != null && !serviceToken.isBlank()) headers.set("X-Payment-Service-Token", serviceToken);
                    }).retrieve().body(OrderSnapshot.class);
            if (response == null || response.id() == null || !orderId.equals(response.id()) || response.status() == null) {
                throw new DependencyUnavailableException("Order Service returned incomplete payment validation data", null);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Order not found: " + orderId);
            throw new DependencyUnavailableException("Order Service payment validation failed", ex);
        } catch (RestClientException ex) {
            throw new DependencyUnavailableException("Order Service payment validation failed", ex);
        }
    }

    private void addHeaders(HttpHeaders headers, String authorizationHeader) {
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        if (serviceToken != null && !serviceToken.isBlank()) {
            headers.set("X-Payment-Service-Token", serviceToken);
        }
    }
}
