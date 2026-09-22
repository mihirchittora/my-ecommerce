package com.shop.payment.refund;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);

    List<Refund> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}
