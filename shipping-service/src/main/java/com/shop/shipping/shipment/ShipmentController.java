package com.shop.shipping.shipment;

import com.shop.shipping.api.ShippingDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@Tag(name = "Shipments", description = "Shipment creation, operations, customer visibility, and tracking")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/shipments")
public class ShipmentController {
    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @Operation(summary = "List shipments for shipping operations")
    @GetMapping
    public Page<ShippingDtos.ShipmentSummaryResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Spring sort expression", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        return service.list(page, size, sort, authentication);
    }

    @Operation(summary = "List only the authenticated customer's shipments")
    @GetMapping("/my")
    public Page<ShippingDtos.ShipmentSummaryResponse> my(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        return service.my(page, size, sort, authentication);
    }

    @Operation(summary = "Create a shipment", description = "Validates Order state, exact Inventory reservation/unit references, and idempotency before calling the configured carrier.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShippingDtos.ShipmentResponse create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ShippingDtos.CreateShipmentRequest request,
            Authentication authentication) {
        return service.get(service.create(request, idempotencyKey, authentication).getId(), authentication);
    }

    @Operation(summary = "Get shipment details")
    @GetMapping("/{id}")
    public ShippingDtos.ShipmentResponse get(@PathVariable UUID id, Authentication authentication) {
        return service.get(id, authentication);
    }

    @Operation(summary = "Get customer-safe tracking information")
    @GetMapping("/{id}/tracking")
    public ShippingDtos.TrackingResponse tracking(@PathVariable UUID id, Authentication authentication) {
        return service.tracking(id, authentication);
    }

    @Operation(summary = "Cancel a shipment before carrier handoff")
    @PostMapping("/{id}/cancel")
    public ShippingDtos.ShipmentResponse cancel(@PathVariable UUID id, Authentication authentication) {
        ShipmentEntity shipment = service.cancel(id, authentication);
        return service.get(shipment.getId(), authentication);
    }

    @Operation(summary = "Retry a failed Order shipment notification")
    @PostMapping("/{id}/order-notification/retry")
    public ShippingDtos.ShipmentResponse retryOrderNotification(@PathVariable UUID id, Authentication authentication) {
        ShipmentEntity shipment = service.retryOrderNotification(id, authentication);
        return service.get(shipment.getId(), authentication);
    }

}
