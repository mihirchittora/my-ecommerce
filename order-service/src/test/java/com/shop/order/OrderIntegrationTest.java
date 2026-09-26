package com.shop.order;

import com.shop.order.api.OrderDtos;
import com.shop.order.client.CatalogClient;
import com.shop.order.client.CatalogSku;
import com.shop.order.client.InventoryClient;
import com.shop.order.client.InventoryReservation;
import com.shop.order.domain.OrderRepository;
import com.shop.order.exception.ConflictException;
import com.shop.order.service.OrderApplicationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
class OrderIntegrationTest {
    private static final OrderDtos.ShippingAddressRequest SHIPPING_ADDRESS =
            new OrderDtos.ShippingAddressRequest(null, "Mihir Chittora", "+919999999999", "1 Main Street",
                    null, "Bengaluru", "Karnataka", "560001", "IN", null);
    static {
        PortableDockerEnvironment.configure();
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("order_db")
            .withUsername("order")
            .withPassword("order");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.enabled", () -> "false");
    }

    @BeforeAll
    static void dockerEnvironmentIsConfigured() {
        // Configure Colima/Docker Desktop before Testcontainers creates PostgreSQL.
    }

    @Autowired OrderApplicationService orders;
    @Autowired OrderRepository orderRepository;
    @MockBean CatalogClient catalog;
    @MockBean InventoryClient inventory;

    @Test
    void persistsHistoricalPriceSnapshotAndInventoryReferences() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        CatalogSku originalPrice = new CatalogSku("IP17-BLK-256", productId, variantId, "iPhone 17 Pro", "Black / 256GB",
                Map.of("color", "Black", "storage", "256GB"), new BigDecimal("129900.00"), "INR", true);
        CatalogSku changedPrice = new CatalogSku("IP17-BLK-256", productId, variantId, "iPhone 17 Pro", "Black / 256GB",
                Map.of("color", "Black", "storage", "256GB"), new BigDecimal("149900.00"), "INR", true);
        when(catalog.getSellableSku("IP17-BLK-256")).thenReturn(originalPrice, changedPrice);
        when(inventory.reserve(eq("IP17-BLK-256"), any(), anyLong(), any(), any())).thenAnswer(invocation ->
                new InventoryReservation(reservationId, "IP17-BLK-256", null, invocation.getArgument(2),
                        invocation.getArgument(3), "ACTIVE", Instant.now().plusSeconds(900),
                        invocation.<Long>getArgument(2) == 2
                                ? List.of(new InventoryReservation.ReservedUnit(unitId, "U001", "RESERVED"),
                                new InventoryReservation.ReservedUnit(UUID.randomUUID(), "U002", "RESERVED"))
                                : List.of(new InventoryReservation.ReservedUnit(unitId, "U001", "RESERVED"))));

        var authentication = new TestingAuthenticationToken("customer-1", null, "ROLE_CUSTOMER");
        var response = orders.create(new OrderDtos.CreateOrderRequest("INR",
                List.of(new OrderDtos.CreateOrderItemRequest("IP17-BLK-256", 2)), null, SHIPPING_ADDRESS),
                "checkout-abc123", authentication);

        assertEquals("PENDING_PAYMENT", response.status().name());
        assertEquals("Mihir Chittora", response.shippingAddress().recipientName());
        assertEquals("1 Main Street", response.shippingAddress().line1());
        assertEquals("IN", response.shippingAddress().country());
        assertEquals(new BigDecimal("129900.00"), response.items().getFirst().unitPrice());
        assertEquals(new BigDecimal("259800.00"), response.totalAmount());
        var staffAuthentication = new TestingAuthenticationToken("staff-1", null, "ORDER_READ");
        var staffResponse = orders.get(response.id(), staffAuthentication);
        assertEquals("U001", staffResponse.items().getFirst().inventoryUnits().getFirst().unitCode());
        assertEquals("U002", staffResponse.items().getFirst().inventoryUnits().get(1).unitCode());
        assertEquals(1, orderRepository.findAll().size());
        assertEquals(reservationId, orderRepository.findDetailedById(response.id()).orElseThrow()
                .getItems().getFirst().getReservationId());
        assertEquals("1 Main Street", orderRepository.findDetailedById(response.id()).orElseThrow()
                .getShippingAddress().getLine1());

        var retry = orders.create(new OrderDtos.CreateOrderRequest("INR",
                List.of(new OrderDtos.CreateOrderItemRequest("IP17-BLK-256", 2)), null, SHIPPING_ADDRESS),
                "checkout-abc123", authentication);
        assertEquals(response.id(), retry.id());
        verify(inventory, times(1)).reserve(eq("IP17-BLK-256"), any(), eq(2L), any(), any());
        assertThrows(ConflictException.class, () -> orders.create(new OrderDtos.CreateOrderRequest("INR",
                List.of(new OrderDtos.CreateOrderItemRequest("IP17-BLK-256", 1)), null, SHIPPING_ADDRESS),
                "checkout-abc123", authentication));

        var second = orders.create(new OrderDtos.CreateOrderRequest("INR",
                List.of(new OrderDtos.CreateOrderItemRequest("IP17-BLK-256", 1)), null, SHIPPING_ADDRESS),
                "checkout-price-change", authentication);
        assertEquals(new BigDecimal("149900.00"), second.items().getFirst().unitPrice());
        assertEquals(new BigDecimal("129900.00"), orders.get(response.id(), authentication).items().getFirst().unitPrice());

        var cancelled = orders.cancel(response.id(), authentication);
        assertEquals("CANCELLED", cancelled.status().name());
        assertEquals("ORDER_CANCELLED", cancelled.history().getLast().eventType());
        verify(inventory, times(1)).release(reservationId);
    }
}
