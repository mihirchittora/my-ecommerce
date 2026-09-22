package com.shop.payment.order;

import com.shop.payment.payment.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderPaymentNotifier {
    private static final Logger log = LoggerFactory.getLogger(OrderPaymentNotifier.class);
    private final RestClient client;
    private final String updatePath;
    private final String serviceToken;

    public OrderPaymentNotifier(RestClient client, String updatePath, String serviceToken) {
        this.client = client;
        this.updatePath = updatePath == null ? "" : updatePath.trim();
        this.serviceToken = serviceToken;
    }

    public void notifyPaymentStateChanged(Payment payment) {
        if (updatePath.isBlank()) {
            log.debug("Order payment callback is disabled; paymentId={} status={}", payment.getId(), payment.getStatus());
            return;
        }
        PaymentStateChanged event = new PaymentStateChanged(payment.getId(), payment.getOrderId(),
                payment.getCustomerId(), payment.getStatus().name(), payment.getAmount(), payment.getCurrency(),
                payment.getProvider(), payment.getProviderPaymentId());
        try {
            client.post().uri(updatePath)
                    .headers(headers -> {
                        if (serviceToken != null && !serviceToken.isBlank()) {
                            headers.set("X-Payment-Service-Token", serviceToken);
                        }
                    })
                    .body(event)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            // Payment state remains authoritative here. A configured production deployment should
            // back this call with an outbox/retry worker; this synchronous notification is best effort.
            log.error("Order payment notification failed for paymentId={} orderId={}", payment.getId(),
                    payment.getOrderId(), ex);
        }
    }

    public record PaymentStateChanged(UUID paymentId, UUID orderId, UUID customerId, String paymentStatus,
                                      BigDecimal amount, String currency, Object provider,
                                      String providerPaymentId) {
    }
}
