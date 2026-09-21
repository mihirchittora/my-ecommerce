package com.shop.customer.address;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(UUID customerId);
    List<CustomerAddress> findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(UUID customerId, AddressType addressType);
    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);
}
