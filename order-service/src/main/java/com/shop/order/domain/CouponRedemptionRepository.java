package com.shop.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {
    long countByCouponIdAndCustomerId(UUID couponId, String customerId);
    Optional<CouponRedemption> findByOrderId(UUID orderId);
}
