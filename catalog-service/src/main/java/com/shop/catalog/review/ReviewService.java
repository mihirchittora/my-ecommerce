package com.shop.catalog.review;

import com.shop.catalog.common.ConflictException;
import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.product.Product;
import com.shop.catalog.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final OrderReviewClient orders;
    private final ProductRepository products;
    public ReviewService(ReviewRepository reviews, OrderReviewClient orders, ProductRepository products) {
        this.reviews = reviews;
        this.orders = orders;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDtos.Response> list(UUID productId, String sku, Pageable pageable) {
        String normalizedSku = normalizeSku(sku);
        return (normalizedSku == null
                ? reviews.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED, pageable)
                : reviews.findByProductIdAndSkuAndStatusOrderByCreatedAtDesc(productId, normalizedSku, ReviewStatus.APPROVED, pageable))
                .map(ReviewDtos.Response::from);
    }
    @Transactional(readOnly = true)
    public ReviewDtos.Summary summary(UUID productId, String sku) {
        String normalizedSku = normalizeSku(sku);
        LinkedHashMap<Integer, Long> distribution = new LinkedHashMap<>();
        (normalizedSku == null ? reviews.distributionForProduct(productId) : reviews.distributionForProductAndSku(productId, normalizedSku))
                .forEach(row -> distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));
        Double average = normalizedSku == null ? reviews.averageForProduct(productId) : reviews.averageForProductAndSku(productId, normalizedSku);
        long count = normalizedSku == null ? reviews.countByProductIdAndStatus(productId, ReviewStatus.APPROVED) : reviews.countByProductIdAndSkuAndStatus(productId, normalizedSku, ReviewStatus.APPROVED);
        return new ReviewDtos.Summary(average == null ? 0 : average, count, distribution);
    }
    @Transactional(readOnly = true)
    public Optional<ReviewDtos.Response> own(UUID productId, String sku, UUID orderItemId, Authentication authentication) {
        String customerId = authentication.getName();
        String normalizedSku = normalizeSku(sku);
        Optional<Review> review = normalizedSku == null
                ? (orderItemId == null ? Optional.empty() : reviews.findByCustomerIdAndProductIdAndOrderItemId(customerId, productId, orderItemId))
                : reviews.findByCustomerIdAndProductIdAndSku(customerId, productId, normalizedSku);
        return review.map(ReviewDtos.Response::from);
    }
    @Transactional
    public ReviewDtos.Response create(UUID productId, ReviewDtos.Request request, Authentication authentication, String authorization) {
        String customerId = authentication.getName();
        String sku = normalizeSku(request.sku());
        if (sku == null ? reviews.existsByCustomerIdAndProductId(customerId, productId) : reviews.existsByCustomerIdAndProductIdAndSku(customerId, productId, sku)) throw new ConflictException("You have already reviewed this product SKU");
        if (!orders.verified(request.orderId(), request.orderItemId(), productId, sku, customerId, authorization)) throw new ConflictException("A delivered purchase is required to review this product SKU");
        Review review = new Review(); review.setProductId(productId); review.setSku(sku); review.setCustomerId(customerId); review.setOrderId(request.orderId()); review.setOrderItemId(request.orderItemId()); review.setRating(request.rating()); review.setTitle(request.title()); review.setComment(request.comment().trim()); review.setVerifiedPurchase(true); review.setStatus(ReviewStatus.PENDING);
        return ReviewDtos.Response.from(reviews.save(review));
    }
    @Transactional
    public ReviewDtos.Response updateOwn(UUID id, ReviewDtos.UpdateRequest request, Authentication authentication) {
        Review review = reviews.findByIdAndCustomerId(id, authentication.getName()).orElseThrow(() -> new NotFoundException("Review not found"));
        review.setRating(request.rating());
        review.setTitle(request.title() == null || request.title().isBlank() ? null : request.title().trim());
        review.setComment(request.comment().trim());
        review.setStatus(ReviewStatus.PENDING);
        return ReviewDtos.Response.from(reviews.save(review));
    }
    @Transactional
    public ReviewDtos.Response moderate(UUID id, ReviewStatus status) { Review review = reviews.findById(id).orElseThrow(() -> new NotFoundException("Review not found")); review.setStatus(status); return ReviewDtos.Response.from(reviews.save(review)); }
    @Transactional
    public ReviewDtos.AdminResponse adminModerate(UUID id, ReviewStatus status) {
        Review review = reviews.findById(id).orElseThrow(() -> new NotFoundException("Review not found"));
        review.setStatus(status);
        review = reviews.save(review);
        return adminResponse(review);
    }
    @Transactional
    public void deleteOwn(UUID id, Authentication authentication) { reviews.delete(reviews.findByIdAndCustomerId(id, authentication.getName()).orElseThrow(() -> new NotFoundException("Review not found"))); }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewDtos.AdminResponse> adminList(String search, ReviewStatus status, Pageable pageable) {
        return reviews.findAll((root, query, builder) -> {
            var p = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) p.add(builder.equal(root.get("status"), status));
            if (search != null && !search.isBlank()) p.add(builder.like(builder.lower(root.get("comment")), "%" + search.trim().toLowerCase() + "%"));
            return builder.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable).map(this::adminResponse);
    }
    private ReviewDtos.AdminResponse adminResponse(Review review) {
        Product product = products.findById(review.getProductId()).orElse(null);
        return ReviewDtos.AdminResponse.from(review, product);
    }
    private String normalizeSku(String sku) { return sku == null || sku.isBlank() ? null : sku.trim().toUpperCase(java.util.Locale.ROOT); }
}
