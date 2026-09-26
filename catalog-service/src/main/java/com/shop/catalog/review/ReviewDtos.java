package com.shop.catalog.review;

import com.shop.catalog.product.Product;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ReviewDtos {
    private ReviewDtos() { }
    public record Request(@NotNull UUID orderId, @NotNull UUID orderItemId, @Size(max = 80) String sku,
                          @Min(1) @Max(5) int rating,
                          @Size(max = 160) String title, @NotBlank @Size(max = 4000) String comment) { }
    public record UpdateRequest(@Min(1) @Max(5) int rating,
                                @Size(max = 160) String title, @NotBlank @Size(max = 4000) String comment) { }
    public record Response(UUID id, UUID productId, String sku, UUID orderId, int rating, String title, String comment,
                           ReviewStatus status, boolean verifiedPurchase, Instant createdAt, Instant updatedAt) {
        static Response from(Review review) { return new Response(review.getId(), review.getProductId(), review.getSku(), review.getOrderId(), review.getRating(), review.getTitle(), review.getComment(), review.getStatus(), review.isVerifiedPurchase(), review.getCreatedAt(), review.getUpdatedAt()); }
    }
    public record AdminResponse(UUID id, UUID productId, String productName, String productSlug, String sku,
                                UUID orderId, UUID orderItemId, String customerId, int rating, String title,
                                String comment, ReviewStatus status, boolean verifiedPurchase, Instant createdAt,
                                Instant updatedAt) {
        static AdminResponse from(Review review, Product product) {
            return new AdminResponse(review.getId(), review.getProductId(), product == null ? null : product.getName(),
                    product == null ? null : product.getSlug(), review.getSku(), review.getOrderId(),
                    review.getOrderItemId(), review.getCustomerId(), review.getRating(), review.getTitle(),
                    review.getComment(), review.getStatus(), review.isVerifiedPurchase(), review.getCreatedAt(),
                    review.getUpdatedAt());
        }
    }
    public record Summary(double averageRating, long reviewCount, Map<Integer, Long> distribution) { }
}
