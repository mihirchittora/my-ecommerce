package com.shop.inventory.location;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class LocationDtos {
    private LocationDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 50)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,49}$",
                    message = "code must contain letters, numbers, '-' or '_'")
            @Schema(example = "MUMBAI")
            String code,
            @NotBlank @Size(max = 150)
            @Schema(example = "Mumbai Warehouse")
            String name,
            LocationStatus status) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 50)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,49}$",
                    message = "code must contain letters, numbers, '-' or '_'")
            String code,
            @NotBlank @Size(max = 150) String name,
            LocationStatus status) {
    }

    public record Response(UUID id, String code, String name, LocationStatus status) {
        static Response from(InventoryLocation location) {
            return new Response(location.getId(), location.getCode(), location.getName(), location.getStatus());
        }
    }
}
