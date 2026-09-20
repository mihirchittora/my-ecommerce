package com.shop.auth.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName) { }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank String password) { }

    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record LogoutRequest(@NotBlank String refreshToken) { }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12, max = 128) String newPassword) { }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) { }

    public record MeResponse(UUID id, String email, String firstName, String lastName,
                             List<String> roles, List<String> permissions) { }
}
