package com.shop.customer.customer;

import com.shop.customer.common.BadRequestException;
import com.shop.customer.common.ForbiddenException;
import com.shop.customer.common.PhoneNormalizer;
import com.shop.customer.common.UnauthorizedException;
import org.springframework.security.core.Authentication;
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
