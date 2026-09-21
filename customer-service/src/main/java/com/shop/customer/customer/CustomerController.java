package com.shop.customer.customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Profile", description = "Authenticated customer profile operations")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my customer profile",
            description = "The Auth JWT subject identifies the profile. The first call lazily creates an empty ACTIVE profile if none exists.")
    @ApiResponse(responseCode = "200", description = "Customer profile")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Customer profile is INACTIVE or BLOCKED")
    public CustomerDtos.CustomerResponse me(Authentication authentication) {
        return CustomerDtos.CustomerResponse.from(customers.getOrCreateActive(authentication));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update my profile",
            description = "Only firstName, lastName, and phone are mutable. Email changes belong to Auth and are rejected as unknown fields.")
    @ApiResponse(responseCode = "200", description = "Updated customer profile")
    @ApiResponse(responseCode = "400", description = "Validation failure or unsupported field")
    public CustomerDtos.CustomerResponse update(@Valid @RequestBody CustomerDtos.ProfilePatchRequest request,
                                                Authentication authentication) {
        return CustomerDtos.CustomerResponse.from(customers.updateProfile(authentication, request));
    }
}
