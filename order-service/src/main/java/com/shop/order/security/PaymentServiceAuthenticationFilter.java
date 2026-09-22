package com.shop.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class PaymentServiceAuthenticationFilter extends OncePerRequestFilter {
    private final String expectedToken;

    public PaymentServiceAuthenticationFilter(@Value("${app.service-auth.payment-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().matches("/internal/orders/[^/]+/(payment-events|payment-validation)");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader("X-Payment-Service-Token");
        if (expectedToken != null && !expectedToken.isBlank() && provided != null
                && MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "payment-service", null, List.of(new SimpleGrantedAuthority("SERVICE_PAYMENT"))));
        }
        chain.doFilter(request, response);
    }
}
