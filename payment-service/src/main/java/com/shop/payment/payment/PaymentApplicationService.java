package com.shop.payment.payment;

import com.shop.payment.attempt.PaymentAttempt;
import com.shop.payment.attempt.PaymentAttemptRepository;
import com.shop.payment.attempt.PaymentAttemptStatus;
import com.shop.payment.common.BadRequestException;
import com.shop.payment.common.ConflictException;
import com.shop.payment.common.DependencyUnavailableException;
import com.shop.payment.common.GatewayException;
import com.shop.payment.common.NotFoundException;
import com.shop.payment.gateway.CreatePaymentRequest;
import com.shop.payment.gateway.CreatePaymentResult;
import com.shop.payment.gateway.GatewayOperationStatus;
import com.shop.payment.gateway.GatewayProvider;
import com.shop.payment.gateway.GatewayRegistry;
import com.shop.payment.gateway.PaymentGateway;
import com.shop.payment.gateway.RefundPaymentRequest;
import com.shop.payment.gateway.RefundResult;
import com.shop.payment.order.OrderClient;
import com.shop.payment.order.OrderPaymentNotifier;
import com.shop.payment.order.OrderSnapshot;
import com.shop.payment.refund.Refund;
import com.shop.payment.refund.RefundRepository;
import com.shop.payment.refund.RefundStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentApplicationService {
    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final RefundRepository refunds;
    private final OrderClient orderClient;
    private final GatewayRegistry gateways;
    private final OrderPaymentNotifier orderNotifier;
    private final GatewayProvider defaultProvider;
    private final Set<String> supportedCurrencies;

    public PaymentApplicationService(PaymentRepository payments, PaymentAttemptRepository attempts,
                                     RefundRepository refunds, OrderClient orderClient, GatewayRegistry gateways,
                                     OrderPaymentNotifier orderNotifier,
                                     @Value("${app.gateway.default-provider:SANDBOX}") String defaultProvider,
                                     @Value("${app.currencies:INR,USD,EUR}") String currencies) {
        this.payments = payments;
        this.attempts = attempts;
        this.refunds = refunds;
        this.orderClient = orderClient;
        this.gateways = gateways;
        this.orderNotifier = orderNotifier;
        this.defaultProvider = GatewayProvider.parse(defaultProvider);
        this.supportedCurrencies = Arrays.stream(currencies.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(noRollbackFor = GatewayException.class)
    public PaymentDtos.PaymentResponse create(PaymentDtos.CreatePaymentRequest request, String idempotencyKey,
                                              Authentication authentication, String authorizationHeader) {
        UUID customerId = customerId(authentication);
        String key = normalizeIdempotencyKey(idempotencyKey);
        Payment existing = payments.findByCustomerIdAndIdempotencyKey(customerId, key).orElse(null);
        if (existing != null) {
            if (!existing.getOrderId().equals(request.orderId())) {
                throw new ConflictException("Idempotency-Key was already used for another order");
            }
            return response(existing);
        }

        OrderSnapshot order = orderClient.getOrder(request.orderId(), authorizationHeader);
        validateOrder(order, request.orderId(), customerId);
        GatewayProvider provider = parseProvider(request.preferredProvider());
        Payment payment = new Payment();
        payment.setOrderId(order.id());
        payment.setCustomerId(customerId);
        payment.setAmount(money(order.totalAmount()));
        payment.setCurrency(normalizeCurrency(order.currency()));
        payment.setStatus(PaymentStatus.CREATED);
        payment.setProvider(provider);
        payment.setIdempotencyKey(key);
        PaymentAttempt attempt = newAttempt(payment, 1, provider);
        attempt.setIdempotencyKey(key);
        payment.addAttempt(attempt);
        payments.saveAndFlush(payment);

        PaymentGateway gateway = gateways.get(provider);
        try {
            CreatePaymentResult result = gateway.createPayment(new CreatePaymentRequest(payment.getId(), payment.getOrderId(),
                    payment.getAmount(), payment.getCurrency(), request.paymentMethodType()));
            applyCreateResult(payment, attempt, result);
            payments.saveAndFlush(payment);
            orderNotifier.notifyPaymentStateChanged(payment);
            return response(payment);
        } catch (GatewayException ex) {
            markGatewayFailure(payment, attempt, ex.getMessage());
            payments.saveAndFlush(payment);
            throw ex;
        } catch (RuntimeException ex) {
            markGatewayFailure(payment, attempt, ex.getMessage());
            payments.saveAndFlush(payment);
            throw new GatewayException("Payment gateway could not create a checkout session", ex);
        }
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse get(UUID paymentId, Authentication authentication) {
        Payment payment = payments.findByIdAndCustomerId(paymentId, customerId(authentication))
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        return response(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.PaymentResponse> forOrder(UUID orderId, Authentication authentication) {
        UUID customerId = customerId(authentication);
        return payments.findAllByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(payment -> payment.getCustomerId().equals(customerId))
                .map(this::response).toList();
    }

    @Transactional(noRollbackFor = GatewayException.class)
    public PaymentDtos.PaymentResponse retry(UUID paymentId, String idempotencyKey,
                                             Authentication authentication) {
        return response(retryPayment(paymentId, idempotencyKey, customerId(authentication)));
    }

    @Transactional(noRollbackFor = GatewayException.class)
    public PaymentDtos.AdminPaymentResponse retryAdmin(UUID paymentId, String idempotencyKey) {
        return adminResponse(retryPayment(paymentId, idempotencyKey, null));
    }

    private Payment retryPayment(UUID paymentId, String idempotencyKey, UUID ownerId) {
        String key = normalizeIdempotencyKey(idempotencyKey);
        java.util.Optional<Payment> payment = ownerId == null ? payments.findById(paymentId) : payments.findByIdAndCustomerId(paymentId, ownerId);
        Payment existingPayment = payment.orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        PaymentAttempt existing = attempts.findByPaymentIdAndIdempotencyKey(paymentId, key).orElse(null);
        if (existing != null) return existingPayment;
        if (existingPayment.getStatus() != PaymentStatus.FAILED) {
            throw new ConflictException("Only a failed payment can be retried");
        }
        PaymentAttempt previous = attempts.findTopByPaymentIdOrderByAttemptNumberDesc(paymentId)
                .orElseThrow(() -> new ConflictException("Payment has no attempt to retry"));
        PaymentAttempt attempt = newAttempt(existingPayment, previous.getAttemptNumber() + 1, existingPayment.getProvider());
        attempt.setIdempotencyKey(key);
        attempt.setPayment(existingPayment);
        PaymentStateMachine.requireTransition(existingPayment.getStatus(), PaymentStatus.PENDING);
        existingPayment.setStatus(PaymentStatus.PENDING);
        attempts.saveAndFlush(attempt);
        try {
            PaymentGateway gateway = gateways.get(existingPayment.getProvider());
            CreatePaymentResult result = gateway.createPayment(new CreatePaymentRequest(existingPayment.getId(), existingPayment.getOrderId(),
                    existingPayment.getAmount(), existingPayment.getCurrency(), null));
            applyCreateResult(existingPayment, attempt, result);
            attempts.saveAndFlush(attempt);
            payments.saveAndFlush(existingPayment);
            orderNotifier.notifyPaymentStateChanged(existingPayment);
            return existingPayment;
        } catch (GatewayException ex) {
            markGatewayFailure(existingPayment, attempt, ex.getMessage());
            payments.saveAndFlush(existingPayment);
            throw ex;
        } catch (RuntimeException ex) {
            markGatewayFailure(existingPayment, attempt, ex.getMessage());
            payments.saveAndFlush(existingPayment);
            throw new GatewayException("Payment gateway could not create a retry checkout session", ex);
        }
    }

    @Transactional(noRollbackFor = GatewayException.class)
    public PaymentDtos.PaymentResponse refund(UUID paymentId, PaymentDtos.RefundRequest request,
                                              String idempotencyKey, Authentication authentication) {
        return response(refundPayment(paymentId, request, idempotencyKey, customerId(authentication)));
    }

    @Transactional(noRollbackFor = GatewayException.class)
    public PaymentDtos.AdminPaymentResponse refundAdmin(UUID paymentId, PaymentDtos.RefundRequest request,
                                                        String idempotencyKey) {
        return adminResponse(refundPayment(paymentId, request, idempotencyKey, null));
    }

    private Payment refundPayment(UUID paymentId, PaymentDtos.RefundRequest request, String idempotencyKey, UUID ownerId) {
        String key = normalizeIdempotencyKey(idempotencyKey);
        java.util.Optional<Payment> payment = ownerId == null ? payments.findById(paymentId) : payments.findByIdAndCustomerId(paymentId, ownerId);
        Payment existingPayment = payment.orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        Refund existing = refunds.findByPaymentIdAndIdempotencyKey(paymentId, key).orElse(null);
        if (existing != null) return existingPayment;
        if (existingPayment.getStatus() != PaymentStatus.CAPTURED && existingPayment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ConflictException("Only captured payments can be refunded");
        }
        BigDecimal remaining = money(existingPayment.getAmount().subtract(existingPayment.getRefundedAmount()));
        BigDecimal refundAmount = request.amount() == null ? remaining : money(request.amount());
        if (refundAmount.signum() <= 0 || refundAmount.compareTo(remaining) > 0) {
            throw new ConflictException("Refund amount must be greater than zero and no more than the remaining amount");
        }

        PaymentStatus previousStatus = existingPayment.getStatus();
        Refund refund = new Refund();
        refund.setPayment(existingPayment);
        refund.setAmount(refundAmount);
        refund.setCurrency(existingPayment.getCurrency());
        refund.setStatus(RefundStatus.PENDING);
        refund.setIdempotencyKey(key);
        refund.setReason(request.reason());
        refunds.saveAndFlush(refund);
        PaymentStateMachine.requireTransition(existingPayment.getStatus(), PaymentStatus.REFUND_PENDING);
        existingPayment.setStatus(PaymentStatus.REFUND_PENDING);
        payments.saveAndFlush(existingPayment);

        try {
            PaymentGateway gateway = gateways.get(existingPayment.getProvider());
            RefundResult result = gateway.refundPayment(new RefundPaymentRequest(existingPayment.getProviderPaymentId(),
                    refundAmount, existingPayment.getCurrency(), request.reason()));
            refund.setProviderRefundId(result.providerRefundId());
            if (result.status() == GatewayOperationStatus.SUCCEEDED) {
                completeRefund(existingPayment, refund);
                payments.saveAndFlush(existingPayment);
                orderNotifier.notifyPaymentStateChanged(existingPayment);
            } else if (result.status() == GatewayOperationStatus.FAILED) {
                failRefund(existingPayment, refund, result.failureCode(), result.failureMessage(), previousStatus);
                payments.saveAndFlush(existingPayment);
            }
            return existingPayment;
        } catch (GatewayException ex) {
            failRefund(existingPayment, refund, "GATEWAY_ERROR", ex.getMessage(), previousStatus);
            payments.saveAndFlush(existingPayment);
            throw ex;
        } catch (RuntimeException ex) {
            failRefund(existingPayment, refund, "GATEWAY_ERROR", ex.getMessage(), previousStatus);
            payments.saveAndFlush(existingPayment);
            throw new GatewayException("Payment gateway could not process the refund", ex);
        }
    }

    private void applyCreateResult(Payment payment, PaymentAttempt attempt, CreatePaymentResult result) {
        if (result == null || result.status() == null) throw new GatewayException("Gateway returned no payment result");
        PaymentStateMachine.requireTransition(payment.getStatus(), result.status());
        payment.setProviderPaymentId(result.providerPaymentId());
        payment.setProviderOrderId(result.providerOrderId());
        payment.setCheckoutUrl(result.checkoutUrl());
        payment.setCheckoutToken(result.checkoutToken());
        payment.setStatus(result.status());
        attempt.setProviderPaymentId(result.providerPaymentId());
        attempt.setProviderOrderId(result.providerOrderId());
        attempt.setStatus(attemptStatus(result.status()));
        if (result.status() == PaymentStatus.AUTHORIZED) payment.setAuthorizedAt(Instant.now());
        if (result.status() == PaymentStatus.CAPTURED) {
            Instant now = Instant.now();
            payment.setAuthorizedAt(now);
            payment.setCapturedAt(now);
        }
        if (isTerminalAttempt(attempt.getStatus())) attempt.setCompletedAt(Instant.now());
    }

    private void markGatewayFailure(Payment payment, PaymentAttempt attempt, String message) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedAt(Instant.now());
        attempt.setStatus(PaymentAttemptStatus.FAILED);
        attempt.setFailureCode("GATEWAY_ERROR");
        attempt.setFailureMessage(trim(message));
        attempt.setCompletedAt(Instant.now());
    }

    private void completeRefund(Payment payment, Refund refund) {
        refund.setStatus(RefundStatus.SUCCEEDED);
        refund.setCompletedAt(Instant.now());
        payment.setRefundedAmount(money(payment.getRefundedAmount().add(refund.getAmount())));
        PaymentStatus next = payment.getRefundedAmount().compareTo(payment.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        payment.setStatus(next);
    }

    private void failRefund(Payment payment, Refund refund, String code, String message, PaymentStatus previousStatus) {
        refund.setStatus(RefundStatus.FAILED);
        refund.setFailureCode(code);
        refund.setFailureMessage(trim(message));
        refund.setCompletedAt(Instant.now());
        payment.setStatus(previousStatus);
    }

    private PaymentDtos.PaymentResponse response(Payment payment) {
        List<PaymentDtos.AttemptResponse> attemptResponses = payment.getAttempts().stream()
                .map(PaymentDtos.AttemptResponse::from).toList();
        List<PaymentDtos.RefundResponse> refundResponses = refunds.findAllByPaymentIdOrderByCreatedAtDesc(payment.getId()).stream()
                .map(PaymentDtos.RefundResponse::from).toList();
        return new PaymentDtos.PaymentResponse(payment.getId(), payment.getOrderId(), payment.getCustomerId(),
                payment.getAmount(), payment.getCurrency(), payment.getStatus(), payment.getProvider().name(),
                payment.getProviderPaymentId(), payment.getProviderOrderId(), checkoutUrl(payment),
                checkoutToken(payment), payment.getRefundedAmount(), payment.getCreatedAt(), payment.getUpdatedAt(),
                payment.getAuthorizedAt(), payment.getCapturedAt(), payment.getFailedAt(), payment.getCancelledAt(),
                attemptResponses, refundResponses);
    }

    PaymentDtos.AdminPaymentResponse adminResponse(Payment payment) {
        List<PaymentDtos.AttemptResponse> attemptResponses = payment.getAttempts().stream()
                .map(PaymentDtos.AttemptResponse::from).toList();
        List<PaymentDtos.RefundResponse> refundResponses = refunds.findAllByPaymentIdOrderByCreatedAtDesc(payment.getId()).stream()
                .map(PaymentDtos.RefundResponse::from).toList();
        return new PaymentDtos.AdminPaymentResponse(payment.getId(), payment.getOrderId(), payment.getCustomerId(),
                payment.getAmount(), payment.getCurrency(), payment.getStatus(), payment.getProvider().name(),
                payment.getProviderPaymentId(), payment.getProviderOrderId(), payment.getRefundedAmount(),
                payment.getCreatedAt(), payment.getUpdatedAt(), payment.getAuthorizedAt(), payment.getCapturedAt(),
                payment.getFailedAt(), payment.getCancelledAt(), attemptResponses, refundResponses);
    }

    private String checkoutUrl(Payment payment) {
        return payment.getCheckoutUrl();
    }

    private String checkoutToken(Payment payment) {
        return payment.getCheckoutToken();
    }

    private PaymentAttempt newAttempt(Payment payment, int number, GatewayProvider provider) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setAttemptNumber(number);
        attempt.setProvider(provider);
        attempt.setStatus(PaymentAttemptStatus.CREATED);
        attempt.setAmount(payment.getAmount());
        attempt.setCurrency(payment.getCurrency());
        return attempt;
    }

    private void validateOrder(OrderSnapshot order, UUID requestedOrderId, UUID customerId) {
        if (!requestedOrderId.equals(order.id())) throw new ConflictException("Order Service returned a different order");
        UUID orderCustomer;
        try {
            orderCustomer = UUID.fromString(order.customerId());
        } catch (IllegalArgumentException ex) {
            throw new DependencyUnavailableException("Order Service returned an invalid customer reference", ex);
        }
        if (!customerId.equals(orderCustomer)) throw new NotFoundException("Order not found: " + requestedOrderId);
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw new ConflictException("Order is not ready for payment: " + order.status());
        }
        normalizeCurrency(order.currency());
        if (order.totalAmount() == null || money(order.totalAmount()).signum() <= 0) {
            throw new ConflictException("Order total must be greater than zero");
        }
    }

    private GatewayProvider parseProvider(String raw) {
        GatewayProvider requested = raw == null || raw.isBlank() ? defaultProvider : GatewayProvider.parse(raw);
        gateways.get(requested);
        return requested;
    }

    private String normalizeCurrency(String raw) {
        if (raw == null || raw.isBlank()) throw new BadRequestException("Currency is required");
        String currency = raw.trim().toUpperCase(Locale.ROOT);
        boolean validIso;
        try {
            validIso = currency.length() == 3 && Currency.getInstance(currency).getCurrencyCode().equals(currency);
        } catch (IllegalArgumentException ex) {
            validIso = false;
        }
        if (!validIso || "UMU".equals(currency) || !supportedCurrencies.contains(currency)) {
            throw new BadRequestException("Unsupported payment currency: " + currency);
        }
        return currency;
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) throw new BadRequestException("Payment amount is required");
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Payment amounts must have at most two decimal places");
        }
    }

    private UUID customerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("Authenticated customer identity is required");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Authenticated customer identity is not a UUID");
        }
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank() || raw.trim().length() > 200) {
            throw new BadRequestException("Idempotency-Key is required and must be at most 200 characters");
        }
        return raw.trim();
    }

    private PaymentAttemptStatus attemptStatus(PaymentStatus status) {
        return switch (status) {
            case CREATED -> PaymentAttemptStatus.CREATED;
            case PENDING -> PaymentAttemptStatus.PENDING;
            case AUTHORIZED -> PaymentAttemptStatus.AUTHORIZED;
            case CAPTURED -> PaymentAttemptStatus.CAPTURED;
            case FAILED -> PaymentAttemptStatus.FAILED;
            case CANCELLED -> PaymentAttemptStatus.CANCELLED;
            default -> PaymentAttemptStatus.PENDING;
        };
    }

    private boolean isTerminalAttempt(PaymentAttemptStatus status) {
        return status == PaymentAttemptStatus.AUTHORIZED || status == PaymentAttemptStatus.CAPTURED
                || status == PaymentAttemptStatus.FAILED || status == PaymentAttemptStatus.CANCELLED;
    }

    private String trim(String value) {
        if (value == null) return null;
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
