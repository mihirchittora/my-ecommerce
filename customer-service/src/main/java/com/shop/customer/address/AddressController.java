package com.shop.customer.address;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/me/addresses")
@Tag(name = "Customer Addresses", description = "Authenticated customer address book operations")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {
    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    @Operation(summary = "List my addresses", description = "Returns defaults first, then the newest addresses. Ownership is derived from JWT sub.")
    public List<AddressDtos.AddressResponse> list(Authentication authentication) {
        return addresses.list(authentication).stream().map(AddressDtos.AddressResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my addresses", description = "Returns 404 when the address does not belong to the authenticated customer.")
    public AddressDtos.AddressResponse get(@PathVariable UUID id, Authentication authentication) {
        return AddressDtos.AddressResponse.from(addresses.get(authentication, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an address", description = "The first address of each type becomes default. isDefault=true replaces the existing default transactionally.")
    @ApiResponse(responseCode = "409", description = "Database or business conflict")
    public AddressDtos.AddressResponse create(@Valid @RequestBody AddressDtos.AddressRequest request,
                                              Authentication authentication) {
        return AddressDtos.AddressResponse.from(addresses.create(authentication, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace one of my addresses", description = "All address fields are supplied. A default remains default unless another address is explicitly selected.")
    public AddressDtos.AddressResponse update(@PathVariable UUID id, @Valid @RequestBody AddressDtos.AddressRequest request,
                                              Authentication authentication) {
        return AddressDtos.AddressResponse.from(addresses.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one of my addresses", description = "Deleting a default promotes the newest remaining address of that type, if any.")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        addresses.delete(authentication, id);
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "Set an address as default", description = "Replaces only the default address of this address's type.")
    public AddressDtos.AddressResponse setDefault(@PathVariable UUID id, Authentication authentication) {
        return AddressDtos.AddressResponse.from(addresses.setDefault(authentication, id));
    }
}
