package com.shop.cart;

import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartItem;
import com.shop.cart.domain.CartStatus;
import com.shop.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@DataJpaTest
class CartPersistenceIntegrationTest {
    static {
        PortableDockerEnvironment.configure();
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("cart_db")
            .withUsername("cart")
            .withPassword("cart");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CartRepository carts;

    @Test
    void persistsCartAndItemsWithActiveCustomerConstraint() {
        Cart cart = new Cart();
        cart.setCustomerId("customer-1");
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency("INR");
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());
        cart.setExpiresAt(Instant.now().plusSeconds(3600));
        CartItem item = new CartItem();
        item.setSku("SKU-A");
        item.setQuantity(2);
        cart.addItem(item);

        carts.saveAndFlush(cart);
        Cart loaded = carts.findByCustomerIdAndStatus("customer-1", CartStatus.ACTIVE).orElseThrow();

        assertEquals(1, loaded.getItems().size());
        assertEquals("SKU-A", loaded.getItems().getFirst().getSku());
        assertEquals(2, loaded.getItems().getFirst().getQuantity());
    }

    @Test
    void staffSearchAllowsNullFilters() {
        assertNotNull(carts.searchForStaff(null, null, null, null, PageRequest.of(0, 20)));
    }
}
