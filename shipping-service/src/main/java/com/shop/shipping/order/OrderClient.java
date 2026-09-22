package com.shop.shipping.order;

import com.shop.shipping.common.ConflictException;
import com.shop.shipping.common.DependencyException;
import com.shop.shipping.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderClient {
    private static final Logger log = LoggerFactory.getLogger(OrderClient.class);
    private final RestClient client;
    private final String serviceToken;

    public OrderClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public OrderSnapshot getOrder(UUID orderId) {
        try {
            OrderSnapshot response = client.get().uri("/internal/orders/{orderId}", orderId)
                    .headers(this::addServiceToken).retrieve().body(OrderSnapshot.class);
            if (response == null || response.id() == null || !orderId.equals(response.id())
                    || response.orderNumber() == null || response.customerId() == null
                    || response.status() == null || response.items() == null) {
                throw new DependencyException("Order Service returned an incomplete order");
            }
            if (response.shippingAddress() == null) {
                throw new ConflictException("Order has no immutable shipping address snapshot");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Order not found: " + orderId);
            log.warn("Order lookup failed orderId={} status={}", orderId, ex.getStatusCode().value());
            throw new DependencyException("Order Service lookup failed", ex);
        } catch (ResourceAccessException ex) {
            throw new DependencyException("Order Service timed out or could not connect", ex);
        } catch (RestClientException ex) {
            throw new DependencyException("Order Service returned a malformed response", ex);
        }
    }

    public void notifyShipment(UUID orderId, String shipmentNumber, String status) {
        try {
            client.post().uri("/internal/orders/{orderId}/shipping-events", orderId)
                    .headers(this::addServiceToken)
                    .body(new ShippingEvent(shipmentNumber, status))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) throw new ConflictException("Order rejected shipping state " + status);
            throw new DependencyException("Order Service rejected shipment notification", ex);
        } catch (RestClientException ex) {
            throw new DependencyException("Order Service notification failed", ex);
        }
    }

    private void addServiceToken(HttpHeaders headers) {
        if (serviceToken != null && !serviceToken.isBlank()) headers.set("X-Shipping-Service-Token", serviceToken);
    }

    public record OrderSnapshot(UUID id, String orderNumber, String customerId, String status,
                                String currency, List<OrderItemSnapshot> items, ShippingAddressSnapshot shippingAddress) { }

    public record OrderItemSnapshot(UUID id, String sku, String productNameSnapshot, long quantity,
                                    UUID reservationId, String reservationReference,
                                    List<InventoryUnitReference> inventoryUnits) { }

    public record InventoryUnitReference(UUID id, String unitCode) { }
    public record ShippingAddressSnapshot(UUID sourceAddressId, String recipientName, String phone, String line1,
                                          String line2, String city, String state, String postalCode,
                                          String country, String landmark) { }
    public record ShippingEvent(String shipmentNumber, String status) { }
}
