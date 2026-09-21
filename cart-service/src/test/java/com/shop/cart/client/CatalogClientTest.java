package com.shop.cart.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogClientTest {
    private MockRestServiceServer server;
    private CatalogClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CatalogClient(builder.build(), "cart-secret");
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void readsCurrentCatalogDisplayFieldsAndUsesCartServiceToken() {
        server.expect(requestTo("http://catalog/internal/catalog/skus/SKU-1"))
                .andExpect(header("X-Cart-Service-Token", "cart-secret"))
                .andRespond(withSuccess("""
                        {"sku":"SKU-1","variantId":"%s","productId":"%s","productName":"Phone",
                         "variantName":"Black / 256GB","attributes":{"color":"Black"},"price":129900.00,
                         "currency":"INR","active":true}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()), MediaType.APPLICATION_JSON));

        assertEquals("Phone", client.requireActive("sku-1").productName());
    }

    @Test
    void distinguishesUnknownAndInactiveSkus() {
        server.expect(requestTo("http://catalog/internal/catalog/skus/MISSING"))
                .andRespond(withStatus(NOT_FOUND));
        server.expect(requestTo("http://catalog/internal/catalog/skus/INACTIVE"))
                .andRespond(withSuccess("""
                        {"sku":"INACTIVE","productName":"Phone","attributes":{},"price":10,
                         "currency":"INR","active":false}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(UnknownSkuException.class, () -> client.getSku("MISSING"));
        assertThrows(InactiveSkuException.class, () -> client.requireActive("INACTIVE"));
    }
}
