package com.shop.cart.service;

import com.shop.cart.api.CartDtos;
import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartStatus;
import com.shop.cart.repository.CartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class CartAdminService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final CartRepository carts;
    private final CartReadService reads;

    public CartAdminService(CartRepository carts, CartReadService reads) {
        this.carts = carts;
        this.reads = reads;
    }

    @Transactional(readOnly = true)
    public Page<CartDtos.CartSummaryResponse> list(String search, CartStatus status, String sku,
                                                   int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), parseSort(sort));
        String normalizedSearch = normalize(search);
        UUID cartId = parseCartId(normalizedSearch);
        String customerSearch = cartId == null ? normalizedSearch : null;
        String normalizedSku = sku == null || sku.isBlank() ? null : sku.trim().toUpperCase(Locale.ROOT);
        return carts.searchForStaff(cartId, customerSearch, status, normalizedSku, pageable).map(reads::summary);
    }

    @Transactional(readOnly = true)
    public CartDtos.CartResponse get(UUID id) {
        Cart cart = carts.findById(id).orElseThrow(() -> new CartApiException(
                org.springframework.http.HttpStatus.NOT_FOUND, "NOT_FOUND", "Cart not found"));
        return reads.response(cart);
    }

    private Sort parseSort(String raw) {
        String value = raw == null || raw.isBlank() ? null : raw.trim();
        if (value == null) value = DEFAULT_SORT;
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || !isSortProperty(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new CartApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_SORT",
                    "Sort must use property,direction format");
        }
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
    }

    private boolean isSortProperty(String property) {
        return property.equals("createdAt") || property.equals("updatedAt")
                || property.equals("status") || property.equals("currency") || property.equals("expiresAt");
    }

    private UUID parseCartId(String search) {
        if (search == null) return null;
        try {
            return UUID.fromString(search);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
