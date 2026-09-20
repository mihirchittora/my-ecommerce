package com.shop.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String INTERNAL_SKU_PATH = "/internal/catalog/skus/";

    private final String expectedToken;

    public InventoryServiceAuthenticationFilter(
            @Value("${app.service-auth.inventory-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_SKU_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedToken = request.getHeader(SERVICE_TOKEN_HEADER);
        if (hasValidToken(providedToken)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "inventory-service", null,
                    List.of(new SimpleGrantedAuthority("SERVICE_INVENTORY")));
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
}
