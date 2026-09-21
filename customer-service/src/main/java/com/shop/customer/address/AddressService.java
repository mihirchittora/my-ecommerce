package com.shop.customer.address;

import com.shop.customer.common.BadRequestException;
import com.shop.customer.common.NotFoundException;
import com.shop.customer.common.PhoneNormalizer;
import com.shop.customer.customer.Customer;
import com.shop.customer.customer.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.UUID;

@Service
public class AddressService {
    private final CustomerService customers;
    private final CustomerAddressRepository addresses;

    public AddressService(CustomerService customers, CustomerAddressRepository addresses) {
        this.customers = customers;
        this.addresses = addresses;
    }

    @Transactional
    public List<CustomerAddress> list(Authentication authentication) {
        Customer customer = customers.getOrCreateActive(authentication);
        return addresses.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customer.getId());
    }

    @Transactional
    public CustomerAddress get(Authentication authentication, UUID id) {
        Customer customer = customers.getOrCreateActive(authentication);
        return owned(id, customer.getId());
    }

    @Transactional
    public CustomerAddress create(Authentication authentication, AddressDtos.AddressRequest request) {
        Customer customer = customers.lockActive(subject(authentication));
        List<CustomerAddress> sameType = addresses.findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(
                customer.getId(), request.addressType());
        CustomerAddress address = new CustomerAddress();
        address.setCustomer(customer);
        copy(address, request);
        address.setDefaultAddress(sameType.isEmpty() || Boolean.TRUE.equals(request.isDefault()));
        if (address.isDefaultAddress()) {
            unsetDefaults(sameType);
            addresses.flush();
        }
        return addresses.save(address);
    }

    @Transactional
    public CustomerAddress update(Authentication authentication, UUID id, AddressDtos.AddressRequest request) {
        Customer customer = customers.lockActive(subject(authentication));
        CustomerAddress address = owned(id, customer.getId());
        AddressType previousType = address.getAddressType();
        boolean wasDefault = address.isDefaultAddress();
        copy(address, request);

        List<CustomerAddress> sameType = addresses.findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(
                customer.getId(), request.addressType());
        sameType.removeIf(candidate -> candidate.getId().equals(address.getId()));
        boolean requestedDefault = Boolean.TRUE.equals(request.isDefault());
        if (requestedDefault || sameType.isEmpty()) {
            unsetDefaults(sameType);
            addresses.flush();
            address.setDefaultAddress(true);
        } else if (wasDefault && previousType == request.addressType()) {
            // A default address remains default unless the caller explicitly selects another one.
            address.setDefaultAddress(true);
        } else {
            address.setDefaultAddress(false);
        }
        if (previousType != request.addressType() && wasDefault) {
            ensureTypeHasDefault(customer.getId(), previousType, address.getId());
        }
        return addresses.save(address);
    }

    @Transactional
    public void delete(Authentication authentication, UUID id) {
        Customer customer = customers.lockActive(subject(authentication));
        CustomerAddress address = owned(id, customer.getId());
        AddressType type = address.getAddressType();
        boolean wasDefault = address.isDefaultAddress();
        List<CustomerAddress> replacements = addresses.findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(
                customer.getId(), type);
        replacements.removeIf(candidate -> candidate.getId().equals(id));
        addresses.delete(address);
        if (wasDefault && !replacements.isEmpty()) replacements.get(0).setDefaultAddress(true);
    }

    @Transactional
    public CustomerAddress setDefault(Authentication authentication, UUID id) {
        Customer customer = customers.lockActive(subject(authentication));
        CustomerAddress address = owned(id, customer.getId());
        List<CustomerAddress> sameType = addresses.findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(
                customer.getId(), address.getAddressType());
        unsetDefaults(sameType);
        addresses.flush();
        address.setDefaultAddress(true);
        return addresses.save(address);
    }

    private CustomerAddress owned(UUID id, UUID customerId) {
        return addresses.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found"));
    }

    private void copy(CustomerAddress address, AddressDtos.AddressRequest request) {
        String phone = PhoneNormalizer.normalize(request.phone());
        if (phone == null) throw new BadRequestException("phone must be a valid international phone number");
        address.setAddressType(request.addressType());
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(phone);
        address.setLine1(request.line1().trim());
        address.setLine2(trimToNull(request.line2()));
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim().toUpperCase(java.util.Locale.ROOT));
        address.setLandmark(trimToNull(request.landmark()));
    }

    private void unsetDefaults(List<CustomerAddress> addresses) {
        addresses.forEach(address -> address.setDefaultAddress(false));
    }

    private void ensureTypeHasDefault(UUID customerId, AddressType type, UUID excluded) {
        List<CustomerAddress> addressesOfType = addresses.findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(customerId, type);
        addressesOfType.stream().filter(address -> !address.getId().equals(excluded)).findFirst()
                .ifPresent(address -> address.setDefaultAddress(true));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private UUID subject(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new com.shop.customer.common.UnauthorizedException("Authentication is required");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new com.shop.customer.common.UnauthorizedException("JWT subject must be an Auth user UUID");
        }
    }
}
