package com.shop.cart.api;

import com.shop.cart.service.CartApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@Tag(name = "Cart", description = "Authenticated customer cart operations")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartApplicationService service;

    public CartController(CartApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Get or create the authenticated customer's active cart",
            description = "Catalog enrichment is a current display estimate. The cart stores only SKU and quantity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active cart, including an empty cart when none existed"),
            @ApiResponse(responseCode = "401", description = "Authentication required")})
    @GetMapping
    public CartDtos.CartResponse get(Authentication authentication) {
        return service.getCart(authentication);
    }

    @Operation(summary = "Get the cart summary")
    @GetMapping("/summary")
    public CartDtos.CartResponse summary(Authentication authentication) {
        return service.getCart(authentication);
    }

    @Operation(summary = "Add a SKU to the cart",
            description = "Catalog validates the SKU. Adding to a cart never reserves inventory; repeated SKUs are merged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added or merged"),
            @ApiResponse(responseCode = "400", description = "Invalid SKU or quantity"),
            @ApiResponse(responseCode = "404", description = "SKU not found in Catalog"),
            @ApiResponse(responseCode = "409", description = "Inactive SKU, currency mismatch, or cart limit"),
            @ApiResponse(responseCode = "503", description = "Catalog unavailable")})
    @PostMapping("/items")
    public CartDtos.CartResponse add(@Valid @RequestBody CartDtos.AddItemRequest request,
                                     Authentication authentication) {
        return service.addItem(request, authentication);
    }

    @Operation(summary = "Update an existing cart item")
    @PatchMapping("/items/{itemId}")
    public CartDtos.CartResponse update(@PathVariable UUID itemId,
                                        @Valid @RequestBody CartDtos.UpdateItemRequest request,
                                        Authentication authentication) {
        return service.updateItem(itemId, request, authentication);
    }

    @Operation(summary = "Remove an existing cart item")
    @DeleteMapping("/items/{itemId}")
    public CartDtos.CartResponse remove(@PathVariable UUID itemId, Authentication authentication) {
        return service.removeItem(itemId, authentication);
    }

    @Operation(summary = "Clear all items from the active cart")
    @DeleteMapping
    public CartDtos.CartResponse clear(Authentication authentication) {
        return service.clear(authentication);
    }

    @Operation(summary = "Delegate checkout to Order Service",
            description = "Order Service revalidates Catalog, snapshots current price and the supplied shipping address, and coordinates Inventory reservation. The Idempotency-Key is passed through unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order created and cart converted"),
            @ApiResponse(responseCode = "400", description = "Invalid checkout request"),
            @ApiResponse(responseCode = "409", description = "Empty cart, inactive SKU, insufficient inventory, or checkout conflict"),
            @ApiResponse(responseCode = "503", description = "Catalog or Order unavailable")})
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.OK)
    public CartDtos.CheckoutResponse checkout(
            @Parameter(description = "Stable key reused for retries; passed to Order Service", required = true, example = "cart-checkout-123")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) CartDtos.CheckoutRequest request,
            Authentication authentication) {
        return service.checkout(request, idempotencyKey, authentication);
    }
}
