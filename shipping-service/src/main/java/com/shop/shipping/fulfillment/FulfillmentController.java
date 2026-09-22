package com.shop.shipping.fulfillment;

import com.shop.shipping.api.ShippingDtos;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@Tag(name = "Fulfillments", description = "Operational work associated with an Order")
@SecurityRequirement(name = "bearerAuth")
public class FulfillmentController {
    private final FulfillmentService service;

    public FulfillmentController(FulfillmentService service) { this.service = service; }

    @Operation(summary = "Create a fulfillment from an eligible Order", description = "Internal Order-to-Shipping contract. The Order state is checked over HTTP and its itemized unit references are copied as references only.")
    @PostMapping("/internal/fulfillments")
    @ResponseStatus(HttpStatus.CREATED)
    public ShippingDtos.FulfillmentResponse create(@Valid @RequestBody ShippingDtos.CreateFulfillmentRequest request) {
        FulfillmentEntity fulfillment = service.create(request);
        return service.response(fulfillment);
    }

    @Operation(summary = "Get a fulfillment")
    @GetMapping("/api/v1/fulfillments/{id}")
    public ShippingDtos.FulfillmentResponse get(@PathVariable UUID id, Authentication authentication) {
        return service.get(id, authentication);
    }

    @Operation(summary = "Get the fulfillment linked to an Order")
    @GetMapping("/api/v1/fulfillments/order/{orderId}")
    public ShippingDtos.FulfillmentResponse getByOrder(@PathVariable UUID orderId, Authentication authentication) {
        return service.getByOrder(orderId, authentication);
    }

    @Operation(summary = "List fulfillments for shipping operations")
    @GetMapping("/api/v1/fulfillments")
    public Page<ShippingDtos.FulfillmentResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        return service.list(page, size, sort, authentication);
    }

}
