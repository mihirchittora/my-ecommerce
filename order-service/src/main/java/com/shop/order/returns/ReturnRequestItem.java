package com.shop.order.returns;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "return_request_items") @Getter @Setter @NoArgsConstructor
public class ReturnRequestItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "return_request_id", nullable = false) private ReturnRequest returnRequest;
    @Column(name = "order_item_id", nullable = false) private UUID orderItemId;
    @Column(nullable = false, length = 80) private String sku;
    @Column(nullable = false) private long quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ReturnReason reason;
    @Column(length = 30) private String resolution;
}
