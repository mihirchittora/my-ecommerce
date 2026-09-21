package com.shop.cart.service;

import com.shop.cart.client.OrderClient;
import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartItem;
import com.shop.cart.domain.CartStatus;
import com.shop.cart.repository.CartRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CartWriteService {
    private final CartRepository carts;
    private final String currency;
    private final int maxItems;
    private final long maxQuantityPerSku;
    private final Duration expirationDuration;

    public CartWriteService(CartRepository carts,
                            @Value("${cart.currency:INR}") String currency,
                            @Value("${cart.max-items:100}") int maxItems,
                            @Value("${cart.max-quantity-per-sku:100}") long maxQuantityPerSku,
                            @Value("${cart.expiration-duration:30d}") Duration expirationDuration) {
        this.carts = carts;
        this.currency = currency.trim().toUpperCase(Locale.ROOT);
        this.maxItems = maxItems;
        this.maxQuantityPerSku = maxQuantityPerSku;
        this.expirationDuration = expirationDuration;
        if (maxItems < 1 || maxQuantityPerSku < 1 || expirationDuration.isNegative() || expirationDuration.isZero()) {
            throw new IllegalArgumentException("Cart limits and expiration duration must be positive");
        }
    }

    @Transactional
    public Cart getOrCreateActive(String customerId) {
        Cart existing = carts.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE).orElse(null);
        if (existing != null && !isExpired(existing)) return existing;
        if (existing != null) {
            existing.setStatus(CartStatus.EXPIRED);
            existing.touch();
        }
        return create(customerId);
    }

    @Transactional
    public Cart addItem(String customerId, String rawSku, long quantity) {
        Cart cart = activeForUpdate(customerId);
        String sku = normalizeSku(rawSku);
        CartItem existing = cart.getItems().stream().filter(item -> item.getSku().equals(sku)).findFirst().orElse(null);
        if (existing == null) {
            if (cart.getItems().size() >= maxItems) {
                throw new CartApiException(org.springframework.http.HttpStatus.CONFLICT, "CART_LIMIT_REACHED",
                        "Cart cannot contain more than " + maxItems + " distinct SKUs");
            }
            CartItem item = new CartItem();
            item.setSku(sku);
            item.setQuantity(quantity);
            cart.addItem(item);
        } else {
            long merged;
            try {
                merged = Math.addExact(existing.getQuantity(), quantity);
            } catch (ArithmeticException ex) {
                throw quantityTooLarge(sku);
            }
            validateQuantity(merged, sku);
            existing.setQuantity(merged);
        }
        validateQuantity(existing == null ? quantity : existing.getQuantity(), sku);
        cart.touch();
        return cart;
    }

    @Transactional
    public Cart updateItem(String customerId, UUID itemId, long quantity) {
        Cart cart = activeForUpdate(customerId);
        CartItem item = cart.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> notFound("Cart item not found"));
        validateQuantity(quantity, item.getSku());
        item.setQuantity(quantity);
        cart.touch();
        return cart;
    }

    @Transactional
    public Cart removeItem(String customerId, UUID itemId) {
        Cart cart = activeForUpdate(customerId);
        CartItem item = cart.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> notFound("Cart item not found"));
        cart.getItems().remove(item);
        item.setCart(null);
        cart.touch();
        return cart;
    }

    @Transactional
    public Cart clear(String customerId) {
        Cart cart = activeForUpdate(customerId);
        cart.getItems().clear();
        cart.touch();
        return cart;
    }

    @Transactional
    public CheckoutStart beginCheckout(String customerId, String idempotencyKey) {
        Cart converted = carts.findByCustomerIdAndCheckoutIdempotencyKey(customerId, idempotencyKey).orElse(null);
        if (converted != null && converted.getStatus() == CartStatus.CONVERTED) {
            return CheckoutStart.converted(converted);
        }

        Cart inProgress = carts.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.CHECKOUT_IN_PROGRESS).orElse(null);
        if (inProgress != null) {
            throw new CartApiException(org.springframework.http.HttpStatus.CONFLICT, "CHECKOUT_IN_PROGRESS",
                    "Checkout is already in progress for this customer");
        }
        Cart cart = activeForUpdate(customerId);
        if (cart.getItems().isEmpty()) {
            throw new CartApiException(org.springframework.http.HttpStatus.CONFLICT, "EMPTY_CART", "Cannot checkout an empty cart");
        }
        cart.setStatus(CartStatus.CHECKOUT_IN_PROGRESS);
        cart.setCheckoutIdempotencyKey(idempotencyKey);
        cart.touch();
        List<CheckoutLine> lines = cart.getItems().stream()
                .map(item -> new CheckoutLine(item.getSku(), item.getQuantity())).toList();
        return CheckoutStart.started(cart.getId(), cart.getCurrency(), lines);
    }

    @Transactional
    public Cart markConverted(UUID cartId, String customerId, String idempotencyKey, OrderClient.OrderResponse order) {
        Cart cart = carts.findByIdAndCustomerIdForUpdate(cartId, customerId).orElseThrow(() -> notFound("Cart not found"));
        if (cart.getStatus() == CartStatus.CONVERTED) return cart;
        if (cart.getStatus() != CartStatus.CHECKOUT_IN_PROGRESS
                || !idempotencyKey.equals(cart.getCheckoutIdempotencyKey())) {
            throw new CartApiException(org.springframework.http.HttpStatus.CONFLICT, "CART_STATE_CONFLICT",
                    "Cart is no longer in the checkout state expected by this request");
        }
        cart.setStatus(CartStatus.CONVERTED);
        cart.setConvertedOrderId(order.id());
        cart.setConvertedOrderNumber(order.orderNumber());
        cart.touch();
        return cart;
    }

    @Transactional
    public void recoverCheckout(UUID cartId, String customerId) {
        Cart cart = carts.findByIdAndCustomerIdForUpdate(cartId, customerId).orElse(null);
        if (cart != null && cart.getStatus() == CartStatus.CHECKOUT_IN_PROGRESS) {
            cart.setStatus(CartStatus.ACTIVE);
            cart.setCheckoutIdempotencyKey(null);
            cart.touch();
        }
    }

    @Transactional
    public int expireDueCarts() {
        int count = 0;
        for (Cart cart : carts.findExpiredForUpdate(CartStatus.ACTIVE, Instant.now())) {
            cart.setStatus(CartStatus.EXPIRED);
            cart.touch();
            count++;
        }
        return count;
    }

    private Cart activeForUpdate(String customerId) {
        Cart cart = carts.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE).orElse(null);
        if (cart == null || isExpired(cart)) {
            if (cart != null) {
                cart.setStatus(CartStatus.EXPIRED);
                cart.touch();
            }
            return create(customerId);
        }
        return cart;
    }

    private Cart create(String customerId) {
        Cart cart = new Cart();
        cart.setCustomerId(requireCustomer(customerId));
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency(currency);
        cart.setExpiresAt(Instant.now().plus(expirationDuration));
        return carts.save(cart);
    }

    private boolean isExpired(Cart cart) {
        return cart.getExpiresAt() != null && !cart.getExpiresAt().isAfter(Instant.now());
    }

    private String requireCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new CartApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authenticated customer identity is required");
        }
        return customerId.trim();
    }

    private String normalizeSku(String rawSku) {
        if (rawSku == null || rawSku.isBlank()) throw new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_SKU", "SKU is required");
        String sku = rawSku.trim().toUpperCase(Locale.ROOT);
        if (!sku.matches("^[A-Z0-9][A-Z0-9._-]{0,79}$")) throw new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_SKU", "SKU format is invalid");
        return sku;
    }

    private void validateQuantity(long quantity, String sku) {
        if (quantity <= 0 || quantity > maxQuantityPerSku) throw quantityTooLarge(sku);
    }

    private CartApiException quantityTooLarge(String sku) {
        return new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_QUANTITY",
                "Quantity for SKU " + sku + " must be between 1 and " + maxQuantityPerSku);
    }

    private CartApiException notFound(String message) {
        return new CartApiException(org.springframework.http.HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public record CheckoutLine(String sku, long quantity) {
    }

    public record CheckoutStart(UUID cartId, String currency, List<CheckoutLine> lines,
                                Cart convertedCart) {
        static CheckoutStart started(UUID cartId, String currency, List<CheckoutLine> lines) {
            return new CheckoutStart(cartId, currency, new ArrayList<>(lines), null);
        }

        static CheckoutStart converted(Cart cart) {
            return new CheckoutStart(cart.getId(), cart.getCurrency(), List.of(), cart);
        }

        public boolean alreadyConverted() {
            return convertedCart != null;
        }
    }
}
