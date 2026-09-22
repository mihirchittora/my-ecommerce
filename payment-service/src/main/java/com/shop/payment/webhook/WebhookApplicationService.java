package com.shop.payment.webhook;

import com.shop.payment.attempt.PaymentAttempt;
import com.shop.payment.attempt.PaymentAttemptRepository;
import com.shop.payment.attempt.PaymentAttemptStatus;
import com.shop.payment.common.BadRequestException;
import com.shop.payment.common.ConflictException;
import com.shop.payment.common.NotFoundException;
import com.shop.payment.gateway.GatewayProvider;
import com.shop.payment.gateway.GatewayRegistry;
import com.shop.payment.gateway.GatewayWebhookEvent;
import com.shop.payment.gateway.PaymentGateway;
import com.shop.payment.order.OrderPaymentNotifier;
import com.shop.payment.payment.Payment;
import com.shop.payment.payment.PaymentRepository;
import com.shop.payment.payment.PaymentStateMachine;
import com.shop.payment.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class WebhookApplicationService {
    private final WebhookEventRepository events;
    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final GatewayRegistry gateways;
    private final OrderPaymentNotifier orderNotifier;

    public WebhookApplicationService(WebhookEventRepository events, PaymentRepository payments,
                                     PaymentAttemptRepository attempts, GatewayRegistry gateways,
                                     OrderPaymentNotifier orderNotifier) {
        this.events = events;
        this.payments = payments;
        this.attempts = attempts;
        this.gateways = gateways;
        this.orderNotifier = orderNotifier;
    }

    @Transactional
    public com.shop.payment.payment.PaymentDtos.WebhookResponse process(String providerValue, String signature,
                                                                         String payload) {
        GatewayProvider provider = GatewayProvider.parse(providerValue);
        PaymentGateway gateway = gateways.get(provider);
        if (!gateway.verifyWebhook(payload, signature)) {
            throw new BadRequestException("Webhook signature verification failed");
        }
        GatewayWebhookEvent event = gateway.parseWebhookEvent(payload);
        WebhookEvent existing = events.findByProviderAndProviderEventId(provider, event.providerEventId()).orElse(null);
        if (existing != null) {
            return duplicateResponse(event, existing, payload);
        }

        String payloadHash = sha256(payload);
        int claimed = events.claim(java.util.UUID.randomUUID(), provider.name(), event.providerEventId(),
                event.eventType(), event.providerPaymentId(), event.providerOrderId(), payloadHash);
        WebhookEvent record = events.findByProviderAndProviderEventId(provider, event.providerEventId())
                .orElseThrow(() -> new IllegalStateException("Webhook claim was not persisted"));
        if (claimed == 0) return duplicateResponse(event, record, payload);

        Payment payment = findPayment(provider, event);
        if (payment == null) {
            record.setStatus(WebhookEventStatus.IGNORED);
            record.setProcessedAt(Instant.now());
            events.saveAndFlush(record);
            return new com.shop.payment.payment.PaymentDtos.WebhookResponse(event.providerEventId(),
                    WebhookEventStatus.IGNORED.name(), false);
        }
        validateEvent(payment, event);
        apply(payment, event);
        payments.saveAndFlush(payment);
        record.setStatus(WebhookEventStatus.PROCESSED);
        record.setProcessedAt(Instant.now());
        events.saveAndFlush(record);
        orderNotifier.notifyPaymentStateChanged(payment);
        return new com.shop.payment.payment.PaymentDtos.WebhookResponse(event.providerEventId(),
                WebhookEventStatus.PROCESSED.name(), false);
    }

    private com.shop.payment.payment.PaymentDtos.WebhookResponse duplicateResponse(GatewayWebhookEvent event,
                                                                                     WebhookEvent existing,
                                                                                     String payload) {
        if (!existing.getPayloadHash().equals(sha256(payload))) {
            throw new ConflictException("Provider event ID was already received with a different payload");
        }
        return new com.shop.payment.payment.PaymentDtos.WebhookResponse(event.providerEventId(),
                existing.getStatus().name(), true);
    }

    private Payment findPayment(GatewayProvider provider, GatewayWebhookEvent event) {
        if (event.providerPaymentId() != null) {
            Payment payment = payments.findByProviderAndProviderPaymentId(provider, event.providerPaymentId()).orElse(null);
            if (payment != null) return payment;
        }
        if (event.providerOrderId() != null) {
            return payments.findByProviderAndProviderOrderId(provider, event.providerOrderId()).orElse(null);
        }
        return null;
    }

    private void validateEvent(Payment payment, GatewayWebhookEvent event) {
        if (event.amount() != null && (event.paymentStatus() == PaymentStatus.AUTHORIZED
                || event.paymentStatus() == PaymentStatus.CAPTURED)
                && payment.getAmount().compareTo(event.amount()) != 0) {
            throw new ConflictException("Webhook amount does not match the payment amount");
        }
        if (event.currency() != null && !payment.getCurrency().equalsIgnoreCase(event.currency())) {
            throw new ConflictException("Webhook currency does not match the payment currency");
        }
    }

    private void apply(Payment payment, GatewayWebhookEvent event) {
        PaymentStatus next = event.paymentStatus();
        PaymentStateMachine.requireTransition(payment.getStatus(), next);
        payment.setStatus(next);
        Instant now = Instant.now();
        if (next == PaymentStatus.AUTHORIZED) payment.setAuthorizedAt(now);
        if (next == PaymentStatus.CAPTURED) {
            payment.setAuthorizedAt(payment.getAuthorizedAt() == null ? now : payment.getAuthorizedAt());
            payment.setCapturedAt(now);
        }
        if (next == PaymentStatus.FAILED) payment.setFailedAt(now);
        if (next == PaymentStatus.CANCELLED) payment.setCancelledAt(now);
        if (next == PaymentStatus.REFUNDED) payment.setRefundedAmount(payment.getAmount());
        if (next == PaymentStatus.PARTIALLY_REFUNDED && event.amount() != null) {
            BigDecimal newTotal = payment.getRefundedAmount().add(event.amount());
            if (newTotal.compareTo(payment.getAmount()) > 0) {
                throw new ConflictException("Webhook refund amount exceeds the payment amount");
            }
            payment.setRefundedAmount(newTotal);
        }
        PaymentAttempt attempt = attempts.findTopByPaymentIdOrderByAttemptNumberDesc(payment.getId()).orElse(null);
        if (attempt != null) {
            attempt.setStatus(attemptStatus(next));
            attempt.setProviderPaymentId(event.providerPaymentId());
            attempt.setProviderOrderId(event.providerOrderId());
            attempt.setFailureCode(event.failureCode());
            attempt.setFailureMessage(event.failureMessage());
            if (next == PaymentStatus.AUTHORIZED || next == PaymentStatus.CAPTURED
                    || next == PaymentStatus.FAILED || next == PaymentStatus.CANCELLED) {
                attempt.setCompletedAt(now);
            }
        }
    }

    private PaymentAttemptStatus attemptStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING -> PaymentAttemptStatus.PENDING;
            case AUTHORIZED -> PaymentAttemptStatus.AUTHORIZED;
            case CAPTURED -> PaymentAttemptStatus.CAPTURED;
            case FAILED -> PaymentAttemptStatus.FAILED;
            case CANCELLED -> PaymentAttemptStatus.CANCELLED;
            default -> PaymentAttemptStatus.PENDING;
        };
    }

    private String sha256(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
