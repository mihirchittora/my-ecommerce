package com.shop.auth.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, JWT refresh, logout, and password changes")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer", description = "Public registration always assigns the CUSTOMER role.")
    public AuthDtos.MeResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token")
    public AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return auth.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a refresh token")
    @SecurityRequirement(name = "bearerAuth")
    public void logout(Authentication authentication, @Valid @RequestBody AuthDtos.LogoutRequest request) {
        auth.logout(authentication, request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user")
    @SecurityRequirement(name = "bearerAuth")
    public AuthDtos.MeResponse me(Authentication authentication) { return auth.me(authentication); }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change the current user's password")
    @SecurityRequirement(name = "bearerAuth")
    public void changePassword(Authentication authentication, @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        auth.changePassword(authentication, request);
    }
}
