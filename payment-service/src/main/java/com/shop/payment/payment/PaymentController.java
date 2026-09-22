package com.shop.payment.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@Tag(name = "Payments", description = "Payment orchestration and provider references")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentApplicationService service;

    public PaymentController(PaymentApplicationService service) { this.service = service; }

    @Operation(summary = "Create an idempotent payment attempt for an existing order",
            description = "The order total and customer identity are obtained from Order Service and JWT respectively.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDtos.PaymentResponse create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody PaymentDtos.CreatePaymentRequest request,
            Authentication authentication) {
        return service.create(request, idempotencyKey, authentication, authorizationHeader);
    }

    @Operation(summary = "Get a payment owned by the authenticated customer")
    @GetMapping("/{paymentId}")
    public PaymentDtos.PaymentResponse get(@PathVariable UUID paymentId, Authentication authentication) {
        return service.get(paymentId, authentication);
    }

    @Operation(summary = "Retry a failed payment with a new provider attempt")
    @PostMapping("/{paymentId}/retry")
    public PaymentDtos.PaymentResponse retry(@PathVariable UUID paymentId,
                                             @RequestHeader("Idempotency-Key") String idempotencyKey,
                                             Authentication authentication) {
        return service.retry(paymentId, idempotencyKey, authentication);
    }

    @Operation(summary = "List payment attempts for an owned order")
    @GetMapping("/order/{orderId}")
    public List<PaymentDtos.PaymentResponse> forOrder(@PathVariable UUID orderId, Authentication authentication) {
        return service.forOrder(orderId, authentication);
    }

    @Operation(summary = "Request a full or partial refund")
    @PostMapping({"/{paymentId}/refund", "/{paymentId}/refunds"})
    public PaymentDtos.PaymentResponse refund(
            @PathVariable UUID paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) PaymentDtos.RefundRequest request,
            Authentication authentication) {
        PaymentDtos.RefundRequest actual = request == null ? new PaymentDtos.RefundRequest(null, null) : request;
        return service.refund(paymentId, actual, idempotencyKey, authentication);
    }
}
