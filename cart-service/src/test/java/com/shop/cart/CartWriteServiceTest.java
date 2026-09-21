package com.shop.cart;

import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartItem;
import com.shop.cart.domain.CartStatus;
import com.shop.cart.repository.CartRepository;
import com.shop.cart.service.CartWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CartWriteServiceTest {
    private CartRepository repository;
    private CartWriteService service;
    private Cart cart;

    @BeforeEach
    void setUp() {
        repository = (CartRepository) Proxy.newProxyInstance(
                CartRepository.class.getClassLoader(), new Class[]{CartRepository.class}, new RepositoryHandler());
        service = new CartWriteService(repository, "INR", 100, 100, Duration.ofDays(30));
        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setCustomerId("customer-a");
        cart.setCurrency("INR");
        cart.setStatus(CartStatus.ACTIVE);
        cart.setExpiresAt(java.time.Instant.now().plus(Duration.ofDays(1)));
    }

    @Test
    void mergesDuplicateSkuIntoOneRow() {
        service.addItem("customer-a", "sku-a", 2);
        service.addItem("customer-a", "SKU-A", 3);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().getFirst().getQuantity());
        assertEquals("SKU-A", cart.getItems().getFirst().getSku());
    }

    @Test
    void rejectsNonPositiveAndOverLimitQuantities() {
        assertThrows(RuntimeException.class, () -> service.addItem("customer-a", "SKU-A", 0));
        assertThrows(RuntimeException.class, () -> service.addItem("customer-a", "SKU-A", 101));
    }

    @Test
    void checkoutMovesCartToInProgressAndCanRecover() {
        service.addItem("customer-a", "SKU-A", 1);
        var start = service.beginCheckout("customer-a", "checkout-1");

        assertEquals(CartStatus.CHECKOUT_IN_PROGRESS, cart.getStatus());
        assertEquals("checkout-1", cart.getCheckoutIdempotencyKey());
        assertEquals("SKU-A", start.lines().getFirst().sku());

        service.recoverCheckout(cart.getId(), "customer-a");
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertEquals(null, cart.getCheckoutIdempotencyKey());
    }

    @Test
    void convertedCartCannotBeMutatedByActiveMutationPath() {
        cart.setStatus(CartStatus.CONVERTED);
        Cart newCart = new Cart();
        newCart.setId(UUID.randomUUID());

        Cart result = service.addItem("customer-a", "SKU-A", 1);

        assertEquals(CartStatus.ACTIVE, result.getStatus());
        assertEquals(CartStatus.CONVERTED, cart.getStatus());
    }

    private final class RepositoryHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("findByCustomerIdAndStatusForUpdate")) {
                CartStatus status = (CartStatus) args[1];
                return status == CartStatus.ACTIVE && cart.getStatus() == CartStatus.ACTIVE
                        ? Optional.of(cart) : Optional.empty();
            }
            if (method.getName().equals("findByCustomerIdAndCheckoutIdempotencyKey")) {
                String key = (String) args[1];
                return key.equals(cart.getCheckoutIdempotencyKey()) ? Optional.of(cart) : Optional.empty();
            }
            if (method.getName().equals("findByIdAndCustomerIdForUpdate")) return Optional.of(cart);
            if (method.getName().equals("save")) return args[0];
            if (method.getReturnType().equals(boolean.class)) return false;
            if (method.getReturnType().equals(long.class)) return 0L;
            if (method.getReturnType().equals(int.class)) return 0;
            return null;
        }
    }
}
