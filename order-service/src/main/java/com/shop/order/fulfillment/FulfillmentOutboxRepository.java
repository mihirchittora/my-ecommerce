package com.shop.order.fulfillment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FulfillmentOutboxRepository extends JpaRepository<FulfillmentOutbox, UUID> {
    Optional<FulfillmentOutbox> findByOrderId(UUID orderId);

    List<FulfillmentOutbox> findByStatusOrderByCreatedAtAsc(FulfillmentOutboxStatus status, Pageable pageable);
}
