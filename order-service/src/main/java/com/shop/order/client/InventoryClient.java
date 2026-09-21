package com.shop.order.client;

import com.shop.order.exception.ConflictException;
import com.shop.order.exception.InsufficientInventoryException;
import com.shop.order.exception.MalformedDependencyResponseException;
import com.shop.order.exception.RemoteDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class InventoryClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private final RestClient client;
    private final String serviceToken;

    public InventoryClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public InventoryReservation reserve(String sku, UUID preferredLocationId, long quantity,
                                        String referenceId, Instant expiresAt) {
        try {
            InventoryReservation response = client.post()
                    .uri("/api/v1/inventory/{sku}/reservations", sku)
                    .headers(headers -> addServiceToken(headers, "X-Order-Service-Token"))
                    .body(new ReservationRequest(preferredLocationId, quantity, referenceId, expiresAt))
                    .retrieve().body(InventoryReservation.class);
            validate(response, sku, quantity, referenceId);
            return response;
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 409) {
                throw new InsufficientInventoryException("Insufficient inventory for SKU " + sku);
            }
            log.warn("Inventory reservation failed for sku={} status={} referenceId={}", sku, status.value(), referenceId);
            throw new RemoteDependencyException("Inventory", "reservation failed with HTTP " + status.value(), ex);
        } catch (ResourceAccessException ex) {
            log.warn("Inventory reservation unavailable for sku={} referenceId={}", sku, referenceId);
            throw new RemoteDependencyException("Inventory", "reservation timed out or could not connect", ex);
        } catch (RestClientException ex) {
            log.warn("Inventory returned a malformed reservation response for sku={} referenceId={}", sku, referenceId);
            throw new MalformedDependencyResponseException("Inventory", "reservation response could not be decoded");
        }
    }

    public InventoryReservation release(UUID reservationId) {
        try {
            InventoryReservation response = client.post()
                    .uri("/api/v1/inventory/reservations/{reservationId}/release", reservationId)
                    .headers(headers -> addServiceToken(headers, "X-Order-Service-Token"))
                    .retrieve().body(InventoryReservation.class);
            if (response == null || response.reservationId() == null) {
                throw new MalformedDependencyResponseException("Inventory", "release response is incomplete");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ConflictException("Inventory reservation cannot be released in its current state");
            }
            log.warn("Inventory release failed for reservationId={} status={}", reservationId, ex.getStatusCode().value());
            throw new RemoteDependencyException("Inventory", "release failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            log.warn("Inventory release unavailable for reservationId={}", reservationId);
            throw new RemoteDependencyException("Inventory", "release timed out or could not connect", ex);
        } catch (RestClientException ex) {
            log.warn("Inventory returned a malformed release response for reservationId={}", reservationId);
            throw new MalformedDependencyResponseException("Inventory", "release response could not be decoded");
        }
    }

    private void validate(InventoryReservation response, String sku, long quantity, String referenceId) {
        if (response == null || response.reservationId() == null || response.sku() == null
                || response.quantity() != quantity || !referenceId.equals(response.referenceId())
                || response.units() == null || response.units().size() != quantity
                || response.units().stream().anyMatch(unit -> unit == null || unit.unitId() == null
                || unit.unitCode() == null || unit.unitCode().isBlank())) {
            throw new MalformedDependencyResponseException("Inventory", "reservation response is incomplete or inconsistent");
        }
    }

    private void addServiceToken(org.springframework.http.HttpHeaders headers, String header) {
        if (serviceToken != null && !serviceToken.isBlank()) headers.set(header, serviceToken);
    }

    private record ReservationRequest(UUID locationId, long quantity, String referenceId, Instant expiresAt) {
    }
}
