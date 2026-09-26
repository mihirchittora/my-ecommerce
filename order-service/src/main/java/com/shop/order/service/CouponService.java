package com.shop.order.service;

import com.shop.order.domain.Coupon;
import com.shop.order.domain.CouponRedemption;
import com.shop.order.domain.CouponRedemptionRepository;
import com.shop.order.domain.CouponRepository;
import com.shop.order.domain.CouponType;
import com.shop.order.exception.BadRequestException;
import com.shop.order.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class CouponService {
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;

    public CouponService(CouponRepository coupons, CouponRedemptionRepository redemptions) {
        this.coupons = coupons;
        this.redemptions = redemptions;
    }

    @Transactional
    public CouponResult reserve(String rawCode, String customerId, UUID orderId, BigDecimal subtotal) {
        if (rawCode == null || rawCode.isBlank()) return CouponResult.none();
        String code = normalize(rawCode);
        Coupon coupon = coupons.findByCodeForUpdate(code)
                .orElseThrow(() -> new BadRequestException("Coupon code is invalid"));
        CouponResult result = calculate(coupon, customerId, subtotal);
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        coupons.saveAndFlush(coupon);
        CouponRedemption redemption = new CouponRedemption();
        redemption.setCouponId(coupon.getId());
        redemption.setCustomerId(customerId);
        redemption.setOrderId(orderId);
        redemptions.saveAndFlush(redemption);
        return result;
    }

    @Transactional(readOnly = true)
    public CouponResult preview(String rawCode, String customerId, BigDecimal subtotal) {
        if (rawCode == null || rawCode.isBlank()) throw new BadRequestException("Coupon code is required");
        String code = normalize(rawCode);
        Coupon coupon = coupons.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BadRequestException("Coupon code is invalid"));
        return calculate(coupon, customerId, subtotal);
    }

    private CouponResult calculate(Coupon coupon, String customerId, BigDecimal subtotal) {
        Instant now = Instant.now();
        if (!coupon.isActive()) throw new BadRequestException("Coupon code is inactive");
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) throw new BadRequestException("Coupon code is not active yet");
        if (coupon.getExpiresAt() != null && !coupon.getExpiresAt().isAfter(now)) throw new BadRequestException("Coupon code has expired");
        BigDecimal orderSubtotal = money(subtotal);
        if (orderSubtotal.compareTo(money(coupon.getMinimumOrderAmount())) < 0) throw new BadRequestException("Order does not meet the coupon minimum");
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) throw new ConflictException("Coupon usage limit has been reached");
        if (coupon.getPerCustomerLimit() != null && redemptions.countByCouponIdAndCustomerId(coupon.getId(), customerId) >= coupon.getPerCustomerLimit()) {
            throw new ConflictException("Coupon customer usage limit has been reached");
        }
        BigDecimal discount = coupon.getType() == CouponType.PERCENTAGE
                ? orderSubtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : money(coupon.getValue());
        if (coupon.getMaximumDiscount() != null) discount = discount.min(money(coupon.getMaximumDiscount()));
        discount = discount.min(orderSubtotal).setScale(2, RoundingMode.HALF_UP);
        return new CouponResult(coupon.getCode(), discount, coupon.getType().name(), coupon.getValue(), coupon.getMaximumDiscount());
    }

    @Transactional
    public void releaseForOrder(UUID orderId) {
        redemptions.findByOrderId(orderId).ifPresent(redemption -> {
            coupons.findById(redemption.getCouponId()).ifPresent(coupon -> {
                coupon.setUsageCount(Math.max(0, coupon.getUsageCount() - 1));
                coupons.save(coupon);
            });
            redemptions.delete(redemption);
        });
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Coupon> list(String search, Boolean active, org.springframework.data.domain.Pageable pageable) {
        String normalized = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return coupons.findAll((root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!normalized.isBlank()) predicates.add(builder.like(builder.lower(root.get("code")), "%" + normalized + "%"));
            if (active != null) predicates.add(builder.equal(root.get("active"), active));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable);
    }

    public Coupon get(UUID id) { return coupons.findById(id).orElseThrow(() -> new com.shop.order.exception.NotFoundException("Coupon not found: " + id)); }

    @Transactional
    public Coupon save(Coupon coupon) {
        coupon.setCode(normalize(coupon.getCode()));
        if (coupon.getType() == null || coupon.getValue() == null || coupon.getValue().signum() < 0) throw new BadRequestException("Coupon type and nonnegative value are required");
        if (coupon.getType() == CouponType.PERCENTAGE && coupon.getValue().compareTo(BigDecimal.valueOf(100)) > 0) throw new BadRequestException("Percentage coupon cannot exceed 100");
        return coupons.save(coupon);
    }

    @Transactional
    public Coupon setActive(UUID id, boolean active) { Coupon coupon = get(id); coupon.setActive(active); return coupons.save(coupon); }

    private String normalize(String code) { return code.trim().toUpperCase(Locale.ROOT); }
    private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }

    public record CouponResult(String code, BigDecimal discount, String type, BigDecimal value, BigDecimal maximumDiscount) {
        static CouponResult none() { return new CouponResult(null, BigDecimal.ZERO.setScale(2), null, null, null); }
    }
}
