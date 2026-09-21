package com.shop.cart.api;

import com.shop.cart.domain.CartStatus;
import com.shop.cart.service.CartAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@Tag(name = "Cart administration", description = "Read-only staff inspection of customer carts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('CART_READ')")
@RequestMapping("/api/v1/carts")
public class CartAdminController {
    private final CartAdminService service;

    public CartAdminController(CartAdminService service) {
        this.service = service;
    }

    @Operation(summary = "List customer carts for staff")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged customer cart summaries"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "CART_READ is required")})
    @GetMapping
    public Page<CartDtos.CartSummaryResponse> list(
            @Parameter(description = "Exact Cart UUID or customer reference fragment")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CartStatus status,
            @RequestParam(required = false) String sku,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort property and direction, for example updatedAt,desc")
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {
        return service.list(search, status, sku, page, size, sort);
    }

    @Operation(summary = "Get one customer cart for staff")
    @GetMapping("/{cartId}")
    public CartDtos.CartResponse get(@PathVariable UUID cartId) {
        return service.get(cartId);
    }
}
