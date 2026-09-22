package com.shop.order.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

public class ShippingClient {
    private final RestClient client;
    private final String serviceToken;

    public ShippingClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public void createFulfillment(UUID orderId) {
        try {
            client.post().uri("/internal/fulfillments")
                    .headers(headers -> {
                        if (serviceToken != null && !serviceToken.isBlank()) {
                            headers.set("X-Order-Service-Token", serviceToken);
                        }
                    })
                    .body(new CreateFulfillmentRequest(orderId))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new ShippingDependencyException("Shipping Service rejected fulfillment creation with HTTP "
                    + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new ShippingDependencyException("Shipping Service is unavailable", ex);
        }
    }

    private record CreateFulfillmentRequest(UUID orderId) { }

    public static class ShippingDependencyException extends RuntimeException {
        public ShippingDependencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
