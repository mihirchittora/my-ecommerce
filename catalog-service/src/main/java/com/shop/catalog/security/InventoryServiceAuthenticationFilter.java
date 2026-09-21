package com.shop.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InventoryServiceAuthenticationFilter extends OncePerRequestFilter {
    static final String SERVICE_TOKEN_HEADER = "X-Inventory-Service-Token";
    static final String ORDER_SERVICE_TOKEN_HEADER = "X-Order-Service-Token";
    static final String CART_SERVICE_TOKEN_HEADER = "X-Cart-Service-Token";
    private static final String INTERNAL_SKU_PATH = "/internal/catalog/skus/";

    private final String expectedToken;
    private final String expectedOrderToken;
    private final String expectedCartToken;

    @Autowired
    public InventoryServiceAuthenticationFilter(
            @Value("${app.service-auth.inventory-token:}") String expectedToken,
            @Value("${app.service-auth.order-token:}") String expectedOrderToken,
            @Value("${app.service-auth.cart-token:}") String expectedCartToken) {
        this.expectedToken = expectedToken;
        this.expectedOrderToken = expectedOrderToken;
        this.expectedCartToken = expectedCartToken;
    }

    InventoryServiceAuthenticationFilter(String expectedToken, String expectedOrderToken) {
        this(expectedToken, expectedOrderToken, "");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_SKU_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedToken = request.getHeader(SERVICE_TOKEN_HEADER);
        String orderToken = request.getHeader(ORDER_SERVICE_TOKEN_HEADER);
        String cartToken = request.getHeader(CART_SERVICE_TOKEN_HEADER);
        if (hasValidToken(providedToken)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "inventory-service", null,
                    List.of(new SimpleGrantedAuthority("SERVICE_INVENTORY")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (hasValidOrderToken(orderToken)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "order-service", null,
                    List.of(new SimpleGrantedAuthority("SERVICE_ORDER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (hasValidCartToken(cartToken)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "cart-service", null,
                    List.of(new SimpleGrantedAuthority("SERVICE_CART")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasValidToken(String providedToken) {
        if (expectedToken == null || expectedToken.isBlank()
                || providedToken == null || providedToken.isBlank()) return false;
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasValidOrderToken(String providedToken) {
        if (expectedOrderToken == null || expectedOrderToken.isBlank()
                || providedToken == null || providedToken.isBlank()) return false;
        return MessageDigest.isEqual(
                expectedOrderToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasValidCartToken(String providedToken) {
        if (expectedCartToken == null || expectedCartToken.isBlank()
                || providedToken == null || providedToken.isBlank()) return false;
        return MessageDigest.isEqual(
                expectedCartToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
