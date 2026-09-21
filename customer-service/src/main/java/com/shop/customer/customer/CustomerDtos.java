package com.shop.customer.customer;

import com.shop.customer.common.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {
    private CustomerDtos() { }

    public record CustomerResponse(
            UUID id,
            UUID authUserId,
            String firstName,
            String lastName,
            String email,
            String phone,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt) {
        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(customer.getId(), customer.getAuthUserId(), customer.getFirstName(),
                    customer.getLastName(), customer.getEmail(), customer.getPhone(), customer.getStatus(),
                    customer.getCreatedAt(), customer.getUpdatedAt());
        }
    }

    @Schema(description = "Only mutable profile fields are accepted. Auth identity and email are not changed here.")
    public record ProfilePatchRequest(
            @Schema(description = "Profile first name", example = "Mihir")
            @Size(max = 80) String firstName,
            @Schema(description = "Profile last name", example = "Chittora")
            @Size(max = 80) String lastName,
            @Schema(description = "Optional phone number normalized and stored in international format", example = "+919876543210")
            @Size(max = 30) @Phone String phone) { }

    public record AdminProfilePatchRequest(
            @Size(max = 80) String firstName,
            @Size(max = 80) String lastName,
            @Size(max = 30) @Phone String phone) { }

    public record AdminStatusRequest(@NotBlank String status) { }
}
