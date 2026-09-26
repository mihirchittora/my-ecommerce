package com.shop.cart.service;

import com.shop.cart.api.CartDtos;
import com.shop.cart.client.CatalogClient;
import com.shop.cart.client.CatalogSku;
import com.shop.cart.client.OrderClient;
import com.shop.cart.domain.Cart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class CartApplicationService {
    private static final Logger log = LoggerFactory.getLogger(CartApplicationService.class);
    private final CartWriteService writes;
    private final CartReadService reads;
    private final CatalogClient catalog;
    private final OrderClient order;
    private final String currency;

    public CartApplicationService(CartWriteService writes,
                                  CartReadService reads,
                                  CatalogClient catalog,
                                  OrderClient order,
                                  @org.springframework.beans.factory.annotation.Value("${cart.currency:INR}") String currency) {
        this.writes = writes;
        this.reads = reads;
        this.catalog = catalog;
        this.order = order;
        this.currency = currency.trim().toUpperCase(Locale.ROOT);
    }

    public CartDtos.CartResponse getCart(Authentication authentication) {
        Cart cart = writes.getOrCreateActive(subject(authentication));
        return reads.response(cart);
    }

    public CartDtos.CartResponse addItem(CartDtos.AddItemRequest request, Authentication authentication) {
        String customerId = subject(authentication);
        CatalogSku sku = catalog.requireActive(request.sku());
        requireCurrency(sku.currency());
        Cart cart = writes.addItem(customerId, sku.sku(), request.quantity());
        log.info("Cart item added cartId={} customerId={} sku={} quantity={}", cart.getId(), customerId, sku.sku(), request.quantity());
        return reads.response(cart);
    }

    public CartDtos.CartResponse updateItem(UUID itemId, CartDtos.UpdateItemRequest request, Authentication authentication) {
        String customerId = subject(authentication);
        Cart cart = writes.updateItem(customerId, itemId, request.quantity());
        log.info("Cart item updated cartId={} customerId={} itemId={} quantity={}", cart.getId(), customerId, itemId, request.quantity());
        return reads.response(cart);
    }

    public CartDtos.CartResponse removeItem(UUID itemId, Authentication authentication) {
        String customerId = subject(authentication);
        Cart cart = writes.removeItem(customerId, itemId);
        log.info("Cart item removed cartId={} customerId={} itemId={}", cart.getId(), customerId, itemId);
        return reads.response(cart);
    }

    public CartDtos.CartResponse clear(Authentication authentication) {
        String customerId = subject(authentication);
        Cart cart = writes.clear(customerId);
        log.info("Cart cleared cartId={} customerId={}", cart.getId(), customerId);
        return reads.response(cart);
    }

    public CartDtos.CheckoutResponse checkout(CartDtos.CheckoutRequest request, String rawIdempotencyKey,
                                              Authentication authentication) {
        String customerId = subject(authentication);
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        if (request == null || request.shippingAddress() == null) {
            throw new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "SHIPPING_ADDRESS_REQUIRED",
                    "shippingAddress is required at checkout");
        }
        CartWriteService.CheckoutStart start = writes.beginCheckout(customerId, idempotencyKey);
        if (start.alreadyConverted()) {
            Cart converted = start.convertedCart();
            return new CartDtos.CheckoutResponse(converted.getId(), converted.getStatus(),
                    converted.getConvertedOrderId(), converted.getConvertedOrderNumber(), null,
                    "Cart was already converted for this Idempotency-Key");
        }

        try {
            if (request != null && request.currency() != null && !request.currency().isBlank()
                    && !start.currency().equalsIgnoreCase(request.currency().trim())) {
                throw new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH",
                        "Checkout currency must match the cart currency");
            }
            for (CartWriteService.CheckoutLine line : start.lines()) {
                CatalogSku sku = catalog.requireActive(line.sku());
                requireCurrency(sku.currency());
            }

            OrderClient.OrderResponse created = order.create(
                    new OrderClient.CreateOrderRequest(start.currency(),
                            start.lines().stream().map(line -> new OrderClient.CreateOrderItem(line.sku(), line.quantity())).toList(),
                            request == null ? null : request.preferredLocationId(),
                            request == null || request.shippingAddress() == null ? null : new OrderClient.ShippingAddressRequest(
                                    request.shippingAddress().sourceAddressId(), request.shippingAddress().recipientName(),
                                    request.shippingAddress().phone(), request.shippingAddress().line1(),
                                    request.shippingAddress().line2(), request.shippingAddress().city(),
                                    request.shippingAddress().state(), request.shippingAddress().postalCode(),
                                    request.shippingAddress().country(), request.shippingAddress().landmark()),
                            request.paymentMethod(),
                            request.couponCode(), request.serviceLevel()),
                    idempotencyKey, bearerToken(authentication));
            Cart converted = writes.markConverted(start.cartId(), customerId, idempotencyKey, created);
            log.info("Cart checkout converted cartId={} customerId={} orderId={} orderNumber={}",
                    converted.getId(), customerId, created.id(), created.orderNumber());
            return new CartDtos.CheckoutResponse(converted.getId(), converted.getStatus(), created.id(),
                    created.orderNumber(), created.status(), "Order created and cart converted");
        } catch (RuntimeException ex) {
            writes.recoverCheckout(start.cartId(), customerId);
            log.warn("Cart checkout failed cartId={} customerId={} reason={}", start.cartId(), customerId, ex.getMessage());
            throw ex;
        }
    }

    private void requireCurrency(String actual) {
        if (actual == null || !currency.equalsIgnoreCase(actual.trim())) {
            throw new CartApiException(org.springframework.http.HttpStatus.CONFLICT, "CURRENCY_UNSUPPORTED",
                    "Catalog SKU currency must match the cart currency " + currency);
        }
    }

    private String subject(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new CartApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authenticated customer identity is required");
        }
        return authentication.getName();
    }

    private String bearerToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) return jwt.getToken().getTokenValue();
        return null;
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 200) {
            throw new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key is required and must be at most 200 characters");
        }
        return raw.trim();
    }
}
