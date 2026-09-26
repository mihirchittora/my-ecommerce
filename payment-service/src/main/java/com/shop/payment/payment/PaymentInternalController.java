package com.shop.payment.payment;

import com.shop.payment.common.NotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @Hidden @RequestMapping("/internal/payments")
public class PaymentInternalController {
    private final PaymentRepository payments; private final PaymentApplicationService service;
    public PaymentInternalController(PaymentRepository payments, PaymentApplicationService service) { this.payments = payments; this.service = service; }
    @PostMapping("/orders/{orderId}/refund") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('SERVICE_ORDER')")
    public void refund(@PathVariable UUID orderId, @RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody PaymentDtos.RefundRequest request) {
        Payment payment = payments.findAllByOrderIdOrderByCreatedAtDesc(orderId).stream().filter(candidate -> candidate.getStatus() == PaymentStatus.CAPTURED || candidate.getStatus() == PaymentStatus.PARTIALLY_REFUNDED).findFirst().orElseThrow(() -> new NotFoundException("No captured payment exists for order"));
        service.refundAdmin(payment.getId(), request, idempotencyKey);
    }
}
