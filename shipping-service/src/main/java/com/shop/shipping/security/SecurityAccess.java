package com.shop.shipping.security;

import com.shop.shipping.common.ShippingApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public final class SecurityAccess {
    private SecurityAccess() { }

    public static String subject(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
                || authentication.getName().isBlank() || "anonymousUser".equals(authentication.getName())) {
            throw new ShippingApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
        return authentication.getName();
    }

    public static boolean has(Authentication authentication, String permission) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
    }

    public static void require(Authentication authentication, String permission) {
        if (!has(authentication, permission)) {
            throw new ShippingApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient permission");
        }
    }
}
