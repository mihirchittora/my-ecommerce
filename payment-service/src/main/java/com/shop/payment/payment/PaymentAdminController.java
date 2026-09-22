package com.shop.payment.payment;

import com.shop.payment.gateway.GatewayProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@Validated
@Tag(name = "Payment Administration", description = "Permission-protected payment operations for internal users")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/admin/payments")
public class PaymentAdminController {
    private final PaymentAdminService service;
    private final PaymentApplicationService paymentApplication;

    public PaymentAdminController(PaymentAdminService service, PaymentApplicationService paymentApplication) {
        this.service = service;
        this.paymentApplication = paymentApplication;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    @Operation(summary = "List payments for internal operations")
    public Page<PaymentDtos.AdminPaymentResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) GatewayProvider provider,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return service.list(search, status, provider, currency, orderId, customerId, createdFrom, createdTo,
                page, size, sort);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    @Operation(summary = "Get a payment for internal operations")
    public PaymentDtos.AdminPaymentResponse get(@PathVariable UUID paymentId) {
        return service.get(paymentId);
    }

    @PostMapping("/{paymentId}/retry")
    @PreAuthorize("hasAuthority('PAYMENT_RETRY')")
    @Operation(summary = "Retry a failed payment")
    public PaymentDtos.AdminPaymentResponse retry(@PathVariable UUID paymentId,
                                                   @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return paymentApplication.retryAdmin(paymentId, idempotencyKey);
    }

    @PostMapping("/{paymentId}/refunds")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    @Operation(summary = "Request a full or partial refund")
    public PaymentDtos.AdminPaymentResponse refund(
            @PathVariable UUID paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) PaymentDtos.RefundRequest request) {
        PaymentDtos.RefundRequest actual = request == null ? new PaymentDtos.RefundRequest(null, null) : request;
        return paymentApplication.refundAdmin(paymentId, actual, idempotencyKey);
    }
}
