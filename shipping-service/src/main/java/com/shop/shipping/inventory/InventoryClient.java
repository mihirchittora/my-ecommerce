package com.shop.shipping.inventory;

import com.shop.shipping.common.ConflictException;
import com.shop.shipping.common.DependencyException;
import com.shop.shipping.common.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class InventoryClient {
    private final RestClient client;
    private final String serviceToken;

    public InventoryClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public ReservationSnapshot getReservation(UUID reservationId) {
        try {
            ReservationSnapshot response = client.get().uri("/api/v1/inventory/reservations/{id}", reservationId)
                    .headers(this::addServiceToken).retrieve().body(ReservationSnapshot.class);
            if (response == null || response.reservationId() == null || response.units() == null) {
                throw new DependencyException("Inventory Service returned an incomplete reservation");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Reservation not found: " + reservationId);
            if (ex.getStatusCode().value() == 409) throw new ConflictException("Reservation is not usable: " + reservationId);
            throw new DependencyException("Inventory Service lookup failed", ex);
        } catch (ResourceAccessException ex) {
            throw new DependencyException("Inventory Service timed out or could not connect", ex);
        } catch (RestClientException ex) {
            throw new DependencyException("Inventory Service returned a malformed response", ex);
        }
    }

    public ReservationSnapshot shippingTransition(UUID reservationId, String transition,
                                                  String shippingReference, List<UUID> unitIds) {
        try {
            ReservationSnapshot response = client.post()
                    .uri("/api/v1/inventory/reservations/{id}/shipping-transition", reservationId)
                    .headers(this::addServiceToken)
                    .body(new ShippingTransitionRequest(transition, shippingReference, unitIds))
                    .retrieve().body(ReservationSnapshot.class);
            if (response == null || response.reservationId() == null || response.units() == null) {
                throw new DependencyException("Inventory Service returned an incomplete Shipping transition response");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Reservation not found: " + reservationId);
            if (ex.getStatusCode().value() == 409) throw new ConflictException("Inventory rejected Shipping transition for reservation: " + reservationId);
            throw new DependencyException("Inventory Shipping transition failed", ex);
        } catch (ResourceAccessException ex) {
            throw new DependencyException("Inventory Service timed out during Shipping transition", ex);
        } catch (RestClientException ex) {
            throw new DependencyException("Inventory Service returned a malformed Shipping transition response", ex);
        }
    }

    private void addServiceToken(HttpHeaders headers) {
        if (serviceToken != null && !serviceToken.isBlank()) headers.set("X-Shipping-Service-Token", serviceToken);
    }

    public record ReservationSnapshot(UUID reservationId, String sku, long quantity, String referenceId,
                                      String status, Instant expiresAt, List<ReservedUnit> units) { }

    public record ShippingTransitionRequest(String transition, String shippingReference, List<UUID> unitIds) { }
    public record ReservedUnit(UUID unitId, String unitCode, String status) { }
}
