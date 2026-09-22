package com.shop.payment.webhook;

import com.shop.payment.payment.PaymentDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Gateway Webhooks", description = "Verified external gateway callbacks")
@RequestMapping("/api/v1/payments/webhooks")
public class WebhookController {
    private final WebhookApplicationService service;

    public WebhookController(WebhookApplicationService service) { this.service = service; }

    @Operation(summary = "Receive and verify a gateway webhook")
    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentDtos.WebhookResponse receive(
            @PathVariable String provider,
            @RequestHeader(value = "X-Provider-Signature", required = false) String signature,
            @RequestBody String payload) {
        return service.process(provider, signature, payload);
    }
}
