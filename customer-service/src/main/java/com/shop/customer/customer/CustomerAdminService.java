package com.shop.customer.customer;

import com.shop.customer.address.AddressDtos;
import com.shop.customer.address.CustomerAddress;
import com.shop.customer.address.CustomerAddressRepository;
import com.shop.customer.common.ConflictException;
import com.shop.customer.common.BadRequestException;
import com.shop.customer.common.NotFoundException;
import com.shop.customer.common.PhoneNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CustomerAdminService {
    private final CustomerRepository customers;
    private final CustomerAddressRepository addresses;

    public CustomerAdminService(CustomerRepository customers, CustomerAddressRepository addresses) {
        this.customers = customers;
        this.addresses = addresses;
    }

    @Transactional(readOnly = true)
    public Page<CustomerDtos.CustomerResponse> list(String search, CustomerStatus status, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim().toLowerCase(Locale.ROOT);
        return customers.search(normalizedSearch, status, pageable).map(CustomerDtos.CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerDtos.CustomerResponse get(UUID id) {
        return CustomerDtos.CustomerResponse.from(customer(id));
    }

    @Transactional(readOnly = true)
    public CustomerDtos.CustomerResponse getByAuthUserId(UUID authUserId) {
        return CustomerDtos.CustomerResponse.from(customers.findByAuthUserId(authUserId)
                .orElseThrow(() -> new NotFoundException("Customer not found for Auth user: " + authUserId)));
    }

    @Transactional
    public CustomerDtos.CustomerResponse update(UUID id, CustomerDtos.AdminProfilePatchRequest request) {
        Customer customer = customer(id);
        if (request.firstName() != null) customer.setFirstName(normalizeName(request.firstName(), "firstName"));
        if (request.lastName() != null) customer.setLastName(normalizeName(request.lastName(), "lastName"));
        if (request.phone() != null) {
            String phone = PhoneNormalizer.normalize(request.phone());
            if (phone == null) throw new BadRequestException("phone must be a valid international phone number");
            customer.setPhone(phone);
        }
        return CustomerDtos.CustomerResponse.from(customers.save(customer));
    }

    @Transactional
    public CustomerDtos.CustomerResponse status(UUID id, CustomerDtos.AdminStatusRequest request) {
        Customer customer = customer(id);
        try {
            customer.setStatus(CustomerStatus.valueOf(request.status().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported customer status");
        }
        return CustomerDtos.CustomerResponse.from(customers.save(customer));
    }

    @Transactional(readOnly = true)
    public List<AddressDtos.AddressResponse> addresses(UUID id) {
        Customer customer = customer(id);
        return addresses.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customer.getId()).stream()
                .map(AddressDtos.AddressResponse::from).toList();
    }

    private Customer customer(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }

    private String normalizeName(String value, String field) {
        String normalized = value.trim();
        if (normalized.isBlank()) throw new BadRequestException(field + " must not be blank");
        return normalized;
    }
}
