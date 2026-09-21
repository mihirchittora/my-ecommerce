package com.shop.catalog.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryServiceAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validServiceSecretCreatesOnlyServiceAuthority() throws Exception {
        InventoryServiceAuthenticationFilter filter =
                new InventoryServiceAuthenticationFilter("shared-secret", "order-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/catalog/skus/SKU-1");
        request.addHeader(InventoryServiceAuthenticationFilter.SERVICE_TOKEN_HEADER, "shared-secret");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("inventory-service", authentication.getName());
        assertEquals("SERVICE_INVENTORY", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void invalidSecretDoesNotAuthenticateInternalRequest() throws Exception {
        InventoryServiceAuthenticationFilter filter =
                new InventoryServiceAuthenticationFilter("shared-secret", "order-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/catalog/skus/SKU-1");
        request.addHeader(InventoryServiceAuthenticationFilter.SERVICE_TOKEN_HEADER, "wrong-secret");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
