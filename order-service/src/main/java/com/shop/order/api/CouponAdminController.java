package com.shop.order.api;

import com.shop.order.domain.Coupon;
import com.shop.order.service.CouponService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@Tag(name = "Coupon administration")
@SecurityRequirement(name = "bearerAuth")
public class CouponAdminController {
    private final CouponService coupons;

    public CouponAdminController(CouponService coupons) { this.coupons = coupons; }

    @GetMapping
    @PreAuthorize("hasAuthority('COUPON_READ')")
    public Page<CouponDtos.Response> list(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) Boolean active,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return coupons.list(search, active, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))).map(CouponDtos.Response::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_READ')")
    public CouponDtos.Response get(@PathVariable UUID id) { return CouponDtos.Response.from(coupons.get(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    public CouponDtos.Response create(@Valid @RequestBody CouponDtos.Request request) { return CouponDtos.Response.from(coupons.save(toCoupon(request, null))); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    public CouponDtos.Response update(@PathVariable UUID id, @Valid @RequestBody CouponDtos.Request request) {
        return CouponDtos.Response.from(coupons.save(toCoupon(request, coupons.get(id))));
    }

    @PostMapping("/{id}/active")
    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    public CouponDtos.Response setActive(@PathVariable UUID id, @RequestParam boolean active) { return CouponDtos.Response.from(coupons.setActive(id, active)); }

    private Coupon toCoupon(CouponDtos.Request request, Coupon target) {
        Coupon coupon = target == null ? new Coupon() : target;
        coupon.setCode(request.code()); coupon.setType(request.type()); coupon.setValue(request.value());
        coupon.setMinimumOrderAmount(request.minimumOrderAmount() == null ? BigDecimal.ZERO : request.minimumOrderAmount());
        coupon.setMaximumDiscount(request.maximumDiscount()); coupon.setStartsAt(request.startsAt()); coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit()); coupon.setPerCustomerLimit(request.perCustomerLimit());
        if (request.active() != null) coupon.setActive(request.active());
        return coupon;
    }
}
