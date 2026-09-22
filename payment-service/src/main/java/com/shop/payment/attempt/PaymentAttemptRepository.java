package com.shop.payment.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findTopByPaymentIdOrderByAttemptNumberDesc(UUID paymentId);

    Optional<PaymentAttempt> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);
}
