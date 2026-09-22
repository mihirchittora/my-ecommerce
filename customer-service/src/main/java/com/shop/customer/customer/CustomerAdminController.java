package com.shop.customer.customer;

import com.shop.customer.address.AddressDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Administration", description = "Privileged customer profile and address operations")
@SecurityRequirement(name = "bearerAuth")
public class CustomerAdminController {
    private final CustomerAdminService customers;

    public CustomerAdminController(CustomerAdminService customers) {
        this.customers = customers;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "List customers")
    public Page<CustomerDtos.CustomerResponse> list(@RequestParam(required = false) String search,
                                                    @RequestParam(required = false) CustomerStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return customers.list(search, status, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Get a customer")
    public CustomerDtos.CustomerResponse get(@PathVariable UUID id) { return customers.get(id); }

    @GetMapping("/by-auth-user/{authUserId}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Get a customer by Auth user ID")
    public CustomerDtos.CustomerResponse getByAuthUser(@PathVariable UUID authUserId) {
        return customers.getByAuthUserId(authUserId);
    }

    @GetMapping("/{id}/addresses")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "List a customer's addresses")
    public List<AddressDtos.AddressResponse> addresses(@PathVariable UUID id) { return customers.addresses(id); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Update a customer profile")
    public CustomerDtos.CustomerResponse update(@PathVariable UUID id,
                                                @Valid @RequestBody CustomerDtos.AdminProfilePatchRequest request) {
        return customers.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Change customer status")
    public CustomerDtos.CustomerResponse status(@PathVariable UUID id,
                                                @Valid @RequestBody CustomerDtos.AdminStatusRequest request) {
        return customers.status(id, request);
    }
}
