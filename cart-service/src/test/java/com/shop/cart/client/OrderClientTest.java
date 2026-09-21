package com.shop.cart.client;

import com.shop.cart.service.CartApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.CONFLICT;

class OrderClientTest {
    private MockRestServiceServer server;
    private OrderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://order");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrderClient(builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void forwardsIdempotencyKeyAndAuthenticatedBearerToActualOrderContract() {
        UUID orderId = UUID.randomUUID();
        server.expect(requestTo("http://order/api/v1/orders"))
                .andExpect(header("Idempotency-Key", "checkout-1"))
                .andExpect(header("Authorization", "Bearer customer-jwt"))
                .andRespond(withSuccess("""
                        {"id":"%s","orderNumber":"ORD-1","status":"PENDING_PAYMENT"}
                        """.formatted(orderId), MediaType.APPLICATION_JSON));

        OrderClient.OrderResponse response = client.create(
                new OrderClient.CreateOrderRequest("INR", List.of(new OrderClient.CreateOrderItem("SKU-A", 2)), null),
                "checkout-1", "customer-jwt");

        assertEquals(orderId, response.id());
        assertEquals("ORD-1", response.orderNumber());
    }

    @Test
    void mapsOrderConflictToCartConflict() {
        server.expect(requestTo("http://order/api/v1/orders"))
                .andRespond(withStatus(CONFLICT));

        assertThrows(CartApiException.class, () -> client.create(
                new OrderClient.CreateOrderRequest("INR", List.of(new OrderClient.CreateOrderItem("SKU-A", 2)), null),
                "checkout-1", "customer-jwt"));
    }
}
