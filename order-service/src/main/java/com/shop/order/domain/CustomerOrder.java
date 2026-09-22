package com.shop.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false, length = 200)
    private String customerId;

    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "idempotency_payload_hash", length = 64)
    private String idempotencyPayloadHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = jakarta.persistence.FetchType.LAZY, optional = true)
    private OrderShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("createdAt asc")
    private List<OrderHistory> history = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addHistory(OrderHistory event) {
        event.setOrder(this);
        history.add(event);
    }

    public void attachShippingAddress(OrderShippingAddress snapshot) {
        setShippingAddress(snapshot);
    }

    /**
     * Keep the Lombok-generated setter from replacing a checkout snapshot.
     * The relationship is optional only for orders created before the snapshot
     * migration; every new checkout is required to attach one.
     */
    public void setShippingAddress(OrderShippingAddress snapshot) {
        if (shippingAddress != null && shippingAddress != snapshot) {
            throw new IllegalStateException("An order can have only one immutable shipping address snapshot");
        }
        shippingAddress = snapshot;
        if (snapshot != null && snapshot.getOrder() != this) snapshot.setOrder(this);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        normalizeAmounts();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        normalizeAmounts();
    }

    private void normalizeAmounts() {
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (shippingAmount == null) shippingAmount = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
    }
}
