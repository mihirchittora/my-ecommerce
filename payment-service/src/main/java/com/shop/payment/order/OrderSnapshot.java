package com.shop.payment.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSnapshot(UUID id, String customerId, String status, String currency, BigDecimal totalAmount) {
}
