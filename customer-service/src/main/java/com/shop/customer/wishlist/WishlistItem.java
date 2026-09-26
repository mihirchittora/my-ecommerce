package com.shop.customer.wishlist;

import com.shop.customer.customer.Customer;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_customer_product_sku", columnNames = {"customer_id", "product_id", "sku"}))
public class WishlistItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(length = 80)
    private String sku;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
    public WishlistItem() { }
    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Instant getCreatedAt() { return createdAt; }
}
