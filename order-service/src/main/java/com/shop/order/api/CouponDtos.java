package com.shop.order.api;

import com.shop.order.domain.Coupon;
import com.shop.order.domain.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CouponDtos {
    private CouponDtos() { }

    public record Request(@NotBlank @Size(max = 40) String code, @NotNull CouponType type,
                          @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal value,
                          @DecimalMin("0") BigDecimal minimumOrderAmount, @DecimalMin("0") BigDecimal maximumDiscount,
                          Instant startsAt, Instant expiresAt, Long usageLimit, Long perCustomerLimit, Boolean active) {
    }

    public record Response(UUID id, String code, CouponType type, BigDecimal value, BigDecimal minimumOrderAmount,
                           BigDecimal maximumDiscount, Instant startsAt, Instant expiresAt, Long usageLimit,
                           Long perCustomerLimit, long usageCount, boolean active, Instant createdAt, Instant updatedAt) {
        public static Response from(Coupon c) {
            return new Response(c.getId(), c.getCode(), c.getType(), c.getValue(), c.getMinimumOrderAmount(), c.getMaximumDiscount(),
                    c.getStartsAt(), c.getExpiresAt(), c.getUsageLimit(), c.getPerCustomerLimit(), c.getUsageCount(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
