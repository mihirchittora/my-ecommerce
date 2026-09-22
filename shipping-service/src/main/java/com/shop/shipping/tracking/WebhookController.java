package com.shop.shipping.tracking;

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

import java.util.Map;

@RestController
@Tag(name = "Carrier Webhooks", description = "Authenticated and idempotent carrier callbacks")
@RequestMapping("/api/v1/shipping/webhooks")
public class WebhookController {
    private final TrackingService tracking;

    public WebhookController(TrackingService tracking) { this.tracking = tracking; }

    @Operation(summary = "Receive a verified carrier event")
    @PostMapping("/{carrier}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TrackingService.WebhookResult webhook(@PathVariable String carrier,
                                                 @RequestHeader Map<String, String> headers,
                                                 @RequestBody String payload) {
        String path = "/api/v1/shipping/webhooks/" + carrier;
        String signature = header(headers, "x-hmac-signature-v2");
        if (signature == null) signature = header(headers, "x-hmac-signature");
        if (signature == null) signature = header(headers, "x-sandbox-signature");
        return tracking.process(carrier, payload, new com.shop.shipping.carrier.CarrierGateway.WebhookMetadata(
                signature, header(headers, "x-timestamp"), header(headers, "x-path"), "POST"));
    }

    private String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }
}
