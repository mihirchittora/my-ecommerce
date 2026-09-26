package com.shop.order.returns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentRefundClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentRefundClient.class);
    private final RestClient client; private final String token;
    public PaymentRefundClient(@Value("${app.payment.base-url:http://localhost:8087}") String baseUrl, @Value("${app.payment.service-token:}") String token) { this.client = RestClient.builder().baseUrl(baseUrl).build(); this.token = token; }
    public boolean request(UUID orderId, BigDecimal amount, String idempotencyKey, String reason) {
        if (token == null || token.isBlank()) return false;
        try { client.post().uri("/internal/payments/orders/{orderId}/refund", orderId).header("X-Order-Service-Token", token).header("Idempotency-Key", idempotencyKey).body(new RefundBody(amount, reason)).retrieve().toBodilessEntity(); return true; }
        catch (RuntimeException ex) { log.warn("Refund request remains recoverable orderId={} reason={}", orderId, reason); return false; }
    }
    private record RefundBody(BigDecimal amount, String reason) { }
}
