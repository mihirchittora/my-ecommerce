package com.shop.inventory.security;

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
public class OrderServiceAuthenticationFilter extends OncePerRequestFilter {
    private final String expectedToken;

    public OrderServiceAuthenticationFilter(@Value("${app.service-auth.order-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean reserve = "POST".equalsIgnoreCase(request.getMethod())
                && uri.matches("/api/v1/inventory/[^/]+/reservations");
        boolean release = "POST".equalsIgnoreCase(request.getMethod())
                && uri.matches("/api/v1/inventory/reservations/[^/]+/release");
        return !(reserve || release);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader("X-Order-Service-Token");
        if (expectedToken != null && !expectedToken.isBlank() && provided != null
                && MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "order-service", null, List.of(new SimpleGrantedAuthority("SERVICE_ORDER"))));
        }
        filterChain.doFilter(request, response);
    }
}
