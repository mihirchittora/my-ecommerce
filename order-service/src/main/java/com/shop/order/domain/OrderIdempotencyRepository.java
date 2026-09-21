package com.shop.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotency, UUID> {
    Optional<OrderIdempotency> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);
}
