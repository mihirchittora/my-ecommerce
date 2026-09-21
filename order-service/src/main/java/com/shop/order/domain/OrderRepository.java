package com.shop.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<CustomerOrder> {
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByIdAndCustomerId(UUID id, String customerId);

    Page<CustomerOrder> findByCustomerId(String customerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findDetailedById(@Param("id") UUID id);

    Optional<CustomerOrder> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);
}
