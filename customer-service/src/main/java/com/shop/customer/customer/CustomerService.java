package com.shop.customer.customer;

import com.shop.customer.common.BadRequestException;
import com.shop.customer.common.ForbiddenException;
import com.shop.customer.common.PhoneNormalizer;
import com.shop.customer.common.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional
    public Customer getOrCreateActive(Authentication authentication) {
        UUID authUserId = subject(authentication);
        Customer customer = customers.findByAuthUserIdForUpdate(authUserId).orElseGet(() -> create(authUserId));
        synchronizeIdentity(customer, authentication);
        ensureActive(customer);
        return customer;
    }

    @Transactional
    public Customer updateProfile(Authentication authentication, CustomerDtos.ProfilePatchRequest request) {
        Customer customer = getOrCreateActive(authentication);
        if (request.firstName() != null) customer.setFirstName(normalizeName(request.firstName(), "firstName"));
        if (request.lastName() != null) customer.setLastName(normalizeName(request.lastName(), "lastName"));
        if (request.phone() != null) {
            String phone = PhoneNormalizer.normalize(request.phone());
            if (phone == null) throw new BadRequestException("phone must be a valid international phone number");
            customer.setPhone(phone);
        }
        return customers.save(customer);
    }

    @Transactional
    public Customer lockActive(UUID authUserId) {
        Customer customer = customers.findByAuthUserIdForUpdate(authUserId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile was not found"));
        ensureActive(customer);
        return customer;
    }

    private Customer create(UUID authUserId) {
        Instant now = Instant.now();
        customers.insertIfAbsent(UUID.randomUUID(), authUserId, now, now);
        return customers.findByAuthUserIdForUpdate(authUserId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile could not be created"));
    }

    private void ensureActive(Customer customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new ForbiddenException("Customer profile is not active");
        }
    }

    /**
     * Auth owns identity data. Customer keeps a local display copy so customer
     * and admin screens can read it without making a second Auth request.
     * Names are only backfilled when the profile is empty so customer-owned
     * profile edits are not overwritten by an older access token.
     */
    private void synchronizeIdentity(Customer customer, Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) return;
        String email = claim(jwt, "email");
        String firstName = claim(jwt, "firstName");
        String lastName = claim(jwt, "lastName");
        if (!email.isBlank() && !email.equalsIgnoreCase(nullToBlank(customer.getEmail()))) {
            customer.setEmail(email);
        }
        if (isBlank(customer.getFirstName()) && !firstName.isBlank()) customer.setFirstName(firstName);
        if (isBlank(customer.getLastName()) && !lastName.isBlank()) customer.setLastName(lastName);
    }

    private String claim(JwtAuthenticationToken jwt, String name) {
        String value = jwt.getToken().getClaimAsString(name);
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private UUID subject(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("JWT subject must be an Auth user UUID");
        }
    }

    private String normalizeName(String value, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) throw new BadRequestException(field + " must not be blank");
        return normalized;
    }
}
