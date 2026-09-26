package com.shop.catalog.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_customer_product_sku", columnNames = {"customer_id", "product_id", "sku"}))
@Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(length = 80) private String sku;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "order_item_id", nullable = false) private UUID orderItemId;
    @Column(nullable = false) private int rating;
    @Column(length = 160) private String title;
    @Column(nullable = false, length = 4000) private String comment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReviewStatus status;
    @Column(name = "verified_purchase", nullable = false) private boolean verifiedPurchase;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void prePersist() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
