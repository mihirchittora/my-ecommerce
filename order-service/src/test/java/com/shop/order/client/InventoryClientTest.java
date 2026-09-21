package com.shop.order.client;

import com.shop.order.exception.InsufficientInventoryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.CONFLICT;

class InventoryClientTest {
    @Test
    void parsesReservationUnitsAndMapsInsufficientStock() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClient client = new InventoryClient(builder.build(), "inventory-secret");
        UUID reservationId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        server.expect(requestTo("http://inventory/api/v1/inventory/SKU-1/reservations"))
                .andRespond(withSuccess("""
                        {"reservationId":"%s","sku":"SKU-1","locationId":null,"quantity":1,
                         "referenceId":"ORD-1","status":"ACTIVE","expiresAt":"2030-01-01T00:00:00Z",
                         "units":[{"unitId":"%s","unitCode":"U001","status":"RESERVED"}]}
                        """.formatted(reservationId, unitId), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://inventory/api/v1/inventory/SKU-2/reservations"))
                .andRespond(withStatus(CONFLICT));

        InventoryReservation result = client.reserve("SKU-1", null, 1, "ORD-1", null);
        assertEquals(reservationId, result.reservationId());
        assertEquals(unitId, result.units().getFirst().unitId());
        assertEquals("U001", result.units().getFirst().unitCode());

        assertThrows(InsufficientInventoryException.class,
                () -> client.reserve("SKU-2", null, 1, "ORD-2", null));
        server.verify();
    }
}
