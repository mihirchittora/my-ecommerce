package com.shop.payment.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.shop.payment.gateway.GatewayProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    Optional<Payment> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Payment> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findByProviderAndProviderPaymentId(GatewayProvider provider, String providerPaymentId);

    Optional<Payment> findByProviderAndProviderOrderId(GatewayProvider provider, String providerOrderId);
}
