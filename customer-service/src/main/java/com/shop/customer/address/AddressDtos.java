package com.shop.customer.address;

import com.shop.customer.common.IsoCountryCode;
import com.shop.customer.common.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AddressDtos {
    private AddressDtos() { }

    public record AddressRequest(
            @NotNull AddressType addressType,
            @NotBlank @Size(max = 120) String recipientName,
            @NotBlank @Size(max = 30) @Phone String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 120) String city,
            @NotBlank @Size(max = 120) String state,
            @NotBlank @Size(max = 20)
            @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .\\-]{1,19}$", message = "postalCode has an invalid format")
            String postalCode,
            @NotBlank @Size(min = 2, max = 2) @IsoCountryCode String country,
            @Size(max = 200) String landmark,
            @Schema(description = "If true, make this the only default for its address type. The first address of a type is always default.")
            Boolean isDefault) { }

    public record AddressResponse(
            UUID id,
            UUID customerId,
            AddressType addressType,
            String recipientName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            String landmark,
            boolean isDefault,
            Instant createdAt,
            Instant updatedAt) {
        public static AddressResponse from(CustomerAddress address) {
            return new AddressResponse(address.getId(), address.getCustomer().getId(), address.getAddressType(),
                    address.getRecipientName(), address.getPhone(), address.getLine1(), address.getLine2(),
                    address.getCity(), address.getState(), address.getPostalCode(), address.getCountry(),
                    address.getLandmark(), address.isDefaultAddress(), address.getCreatedAt(), address.getUpdatedAt());
        }
    }
}
