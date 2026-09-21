package com.shop.order.api;

import com.shop.order.domain.OrderStatus;
import com.shop.order.service.OrderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.time.Instant;
import java.util.UUID;

@RestController
@Validated
@Tag(name = "Orders", description = "Customer order history and operational order workflows")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderApplicationService service;

    public OrderController(OrderApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Create an order and prepare Inventory reservations",
            description = "Prices, snapshots, totals, and reservation references are calculated by Order Service. "
                    + "Idempotency-Key is required.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created or original idempotent order returned"),
            @ApiResponse(responseCode = "400", description = "Validation or unsupported currency"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Inactive SKU, insufficient inventory, or idempotency conflict"),
            @ApiResponse(responseCode = "503", description = "Catalog or Inventory dependency unavailable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.OrderResponse create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderDtos.CreateOrderRequest request,
            Authentication authentication) {
        return service.create(request, idempotencyKey, authentication);
    }

    @Operation(summary = "List the authenticated customer's orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer order page"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/my")
    public Page<OrderDtos.OrderSummaryResponse> myOrders(
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort property and direction", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        return service.listMy(page, size, sort, authentication);
    }

    @Operation(summary = "List orders for staff with ORDER_READ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered order page"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ORDER_READ is required")
    })
    @GetMapping
    public Page<OrderDtos.OrderSummaryResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort property and direction", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        return service.list(status, orderNumber, customerId, sku, createdFrom, createdTo,
                page, size, sort, authentication);
    }

    @Operation(summary = "Get an order visible to the customer or staff operator")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order details"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Order not found or not owned by the customer")
    })
    @GetMapping("/{orderId}")
    public OrderDtos.OrderResponse get(@PathVariable UUID orderId, Authentication authentication) {
        return service.get(orderId, authentication);
    }

    @Operation(summary = "Cancel an order in a cancellable state")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ORDER_CANCEL is required for another customer's order"),
            @ApiResponse(responseCode = "404", description = "Order not found or not owned by the customer"),
            @ApiResponse(responseCode = "409", description = "Order cannot be cancelled in its current state"),
            @ApiResponse(responseCode = "503", description = "Inventory release unavailable")
    })
    @PostMapping("/{orderId}/cancel")
    public OrderDtos.OrderResponse cancel(@PathVariable UUID orderId, Authentication authentication) {
        return service.cancel(orderId, authentication);
    }
}
