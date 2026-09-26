package com.shop.catalog.review;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

@RestController
@Tag(name = "Product reviews")
public class ReviewController {
    private final ReviewService reviews;
    public ReviewController(ReviewService reviews) { this.reviews = reviews; }
    @GetMapping("/api/v1/products/{productId}/reviews") public Page<ReviewDtos.Response> list(@PathVariable UUID productId, @RequestParam(required = false) String sku, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return reviews.list(productId, sku, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"))); }
    @GetMapping("/api/v1/products/{productId}/reviews/summary") public ReviewDtos.Summary summary(@PathVariable UUID productId, @RequestParam(required = false) String sku) { return reviews.summary(productId, sku); }
    @GetMapping("/api/v1/products/{productId}/reviews/mine") @SecurityRequirement(name = "bearerAuth") public ResponseEntity<ReviewDtos.Response> own(@PathVariable UUID productId, @RequestParam(required = false) String sku, @RequestParam(required = false) UUID orderItemId, Authentication authentication) { return reviews.own(productId, sku, orderItemId, authentication).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build()); }
    @PostMapping("/api/v1/products/{productId}/reviews") @SecurityRequirement(name = "bearerAuth") public ReviewDtos.Response create(@PathVariable UUID productId, @Valid @RequestBody ReviewDtos.Request request, @RequestHeader(value = "Authorization", required = false) String authorization, Authentication authentication) { return reviews.create(productId, request, authentication, authorization); }
    @DeleteMapping("/api/v1/reviews/{reviewId}") @SecurityRequirement(name = "bearerAuth") public void delete(@PathVariable UUID reviewId, Authentication authentication) { reviews.deleteOwn(reviewId, authentication); }
    @PutMapping("/api/v1/reviews/{reviewId}") @SecurityRequirement(name = "bearerAuth") public ReviewDtos.Response update(@PathVariable UUID reviewId, @Valid @RequestBody ReviewDtos.UpdateRequest request, Authentication authentication) { return reviews.updateOwn(reviewId, request, authentication); }
}
