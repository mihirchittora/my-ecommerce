package com.shop.order.returns;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "return_requests") @Getter @Setter @NoArgsConstructor
public class ReturnRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "return_number", nullable = false, unique = true, length = 50) private String returnNumber;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false, length = 200) private String customerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReturnStatus status;
    @Column(nullable = false, length = 1000) private String comment;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2) private BigDecimal refundAmount = BigDecimal.ZERO;
    @Column(name = "refund_status", nullable = false, length = 30) private String refundStatus = "NOT_REQUESTED";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "received_at") private Instant receivedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true) private List<ReturnRequestItem> items = new ArrayList<>();
    public void addItem(ReturnRequestItem item) { item.setReturnRequest(this); items.add(item); }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
