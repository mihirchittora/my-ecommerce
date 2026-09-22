package com.shop.payment.payment;

import com.shop.payment.attempt.PaymentAttempt;
import com.shop.payment.refund.Refund;
import com.shop.payment.refund.RefundStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() { }

    public record CreatePaymentRequest(
            @NotNull UUID orderId,
            @Size(max = 40) String preferredProvider,
            PaymentMethod paymentMethod) {
    }

    public record RefundRequest(
            @DecimalMin(value = "0.01") BigDecimal amount,
            @Size(max = 500) String reason) {
    }

    public record PaymentResponse(
            UUID id,
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            PaymentMethod paymentMethod,
            String provider,
            String providerPaymentId,
            String providerOrderId,
            String checkoutUrl,
            String checkoutToken,
            BigDecimal refundedAmount,
            Instant createdAt,
            Instant updatedAt,
            Instant authorizedAt,
            Instant capturedAt,
            Instant failedAt,
            Instant cancelledAt,
            List<AttemptResponse> attempts,
            List<RefundResponse> refunds) {
    }

    /**
     * Operations users receive provider references and payment history, but never
     * checkout credentials that are only useful to the customer checkout flow.
     */
    public record AdminPaymentResponse(
            UUID id,
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            PaymentMethod paymentMethod,
            String provider,
            String providerPaymentId,
            String providerOrderId,
            BigDecimal refundedAmount,
            Instant createdAt,
            Instant updatedAt,
            Instant authorizedAt,
            Instant capturedAt,
            Instant failedAt,
            Instant cancelledAt,
            List<AttemptResponse> attempts,
            List<RefundResponse> refunds) {
    }

    public record AttemptResponse(
            UUID id,
            int attemptNumber,
            String provider,
            String status,
            String providerPaymentId,
            String providerOrderId,
            String failureCode,
            String failureMessage,
            BigDecimal amount,
            String currency,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
        static AttemptResponse from(PaymentAttempt attempt) {
            return new AttemptResponse(attempt.getId(), attempt.getAttemptNumber(), attempt.getProvider().name(),
                    attempt.getStatus().name(), attempt.getProviderPaymentId(), attempt.getProviderOrderId(),
                    attempt.getFailureCode(), attempt.getFailureMessage(), attempt.getAmount(), attempt.getCurrency(),
                    attempt.getStartedAt(), attempt.getCompletedAt(), attempt.getCreatedAt());
        }
    }

    public record RefundResponse(
            UUID id,
            BigDecimal amount,
            String currency,
            RefundStatus status,
            String providerRefundId,
            String idempotencyKey,
            String reason,
            String failureCode,
            String failureMessage,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt) {
        static RefundResponse from(Refund refund) {
            return new RefundResponse(refund.getId(), refund.getAmount(), refund.getCurrency(), refund.getStatus(),
                    refund.getProviderRefundId(), refund.getIdempotencyKey(), refund.getReason(),
                    refund.getFailureCode(), refund.getFailureMessage(), refund.getCreatedAt(),
                    refund.getUpdatedAt(), refund.getCompletedAt());
        }
    }

    public record WebhookResponse(String providerEventId, String status, boolean duplicate) {
    }
}
