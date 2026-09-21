package com.shop.order.client;

import com.shop.order.exception.InactiveSkuException;
import com.shop.order.exception.MalformedDependencyResponseException;
import com.shop.order.exception.UnknownSkuException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class CatalogClientTest {
    private MockRestServiceServer server;
    private CatalogClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CatalogClient(builder.build(), "catalog-secret");
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void readsPriceAndSnapshotFields() {
        server.expect(requestTo("http://catalog/internal/catalog/skus/SKU-1"))
                .andRespond(withSuccess("""
                        {"sku":"SKU-1","productId":"%s","variantId":"%s","productName":"Phone",
                         "variantName":"Black / 256GB","attributes":{"color":"Black"},"price":129900.00,
                         "currency":"INR","active":true}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()), MediaType.APPLICATION_JSON));

        CatalogSku result = client.getSellableSku("sku-1");

        assertEquals(new BigDecimal("129900.00"), result.price());
        assertEquals("Black / 256GB", result.variantName());
    }

    @Test
    void distinguishesMissingInactiveAndMalformedResponses() {
        server.expect(requestTo("http://catalog/internal/catalog/skus/MISSING"))
                .andRespond(withStatus(NOT_FOUND));
        server.expect(requestTo("http://catalog/internal/catalog/skus/INACTIVE"))
                .andRespond(withSuccess("{" +
                        "\"sku\":\"INACTIVE\",\"productName\":\"Phone\",\"attributes\":{}," +
                        "\"price\":10,\"currency\":\"INR\",\"active\":false}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://catalog/internal/catalog/skus/BAD"))
                .andRespond(withSuccess("{\"sku\":\"BAD\"}", MediaType.APPLICATION_JSON));

        assertThrows(UnknownSkuException.class, () -> client.getSellableSku("MISSING"));
        assertThrows(InactiveSkuException.class, () -> client.getSellableSku("INACTIVE"));
        assertThrows(MalformedDependencyResponseException.class, () -> client.getSellableSku("BAD"));
    }
}
