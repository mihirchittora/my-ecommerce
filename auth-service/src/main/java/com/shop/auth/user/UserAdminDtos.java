package com.shop.auth.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class UserAdminDtos {
    private UserAdminDtos() { }

    public record CreateRequest(@NotBlank @Email @Size(max = 320) String email,
                                @NotBlank @Size(min = 12, max = 128) String password,
                                @NotBlank @Size(max = 80) String firstName,
                                @NotBlank @Size(max = 80) String lastName,
                                List<UUID> roleIds) { }

    public record UpdateRequest(@NotBlank @Size(max = 80) String firstName,
                                @NotBlank @Size(max = 80) String lastName) { }

    public record StatusRequest(@NotBlank String status) { }
    public record RoleAssignmentRequest(@NotNull UUID roleId) { }

    public record Response(UUID id, String email, String firstName, String lastName,
                           UserStatus status, boolean emailVerified, List<String> roles) { }
}
