package com.shop.catalog.review;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@SecurityRequirement(name = "bearerAuth")
public class ReviewAdminController {
    private final ReviewService reviews;
    public ReviewAdminController(ReviewService reviews) { this.reviews = reviews; }
    @GetMapping @PreAuthorize("hasAuthority('REVIEW_READ')") public Page<ReviewDtos.AdminResponse> list(@RequestParam(required = false) String search, @RequestParam(required = false) ReviewStatus status, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return reviews.adminList(search, status, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))); }
    @PostMapping("/{id}/moderate") @PreAuthorize("hasAuthority('REVIEW_MODERATE')") public ReviewDtos.AdminResponse moderate(@PathVariable UUID id, @RequestParam ReviewStatus status) { return reviews.adminModerate(id, status); }
}
