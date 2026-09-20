package com.shop.auth.authentication;

import com.shop.auth.audit.AuditEventType;
import com.shop.auth.audit.AuditService;
import com.shop.auth.common.ConflictException;
import com.shop.auth.common.NotFoundException;
import com.shop.auth.common.UnauthorizedException;
import com.shop.auth.config.AuthProperties;
import com.shop.auth.role.Role;
import com.shop.auth.role.RoleRepository;
import com.shop.auth.token.JwtTokenService;
import com.shop.auth.token.RefreshToken;
import com.shop.auth.token.RefreshTokenRepository;
import com.shop.auth.user.AppUser;
import com.shop.auth.user.UserRepository;
import com.shop.auth.user.UserStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password.";
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokens;
    private final AuthProperties properties;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtTokenService jwtTokens,
                       AuthProperties properties, AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokens = jwtTokens;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional
    public AuthDtos.MeResponse register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("An account already exists for this email");
        Role customer = roles.findByName("CUSTOMER").orElseThrow(() -> new IllegalStateException("CUSTOMER role is not seeded"));
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setRoles(new java.util.LinkedHashSet<>(List.of(customer)));
        user.prepareForPersist();
        AppUser saved = users.save(user);
        audit.record(AuditEventType.USER_REGISTERED, saved.getId(), null);
        return me(saved);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            audit.record(AuditEventType.LOGIN_FAILED, null, "email_hash=" + shortHash(email));
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        Instant now = Instant.now();
        if (isTemporarilyLocked(user, now)) {
            audit.record(AuditEventType.LOGIN_FAILED, user.getId(), "locked=true");
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedLogin(user, now);
            audit.record(AuditEventType.LOGIN_FAILED, user.getId(), null);
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setStatus(UserStatus.ACTIVE);
        user.prepareForPersist();
        AuthDtos.TokenResponse response = issueTokens(user, now, UUID.randomUUID());
        audit.record(AuditEventType.LOGIN_SUCCESS, user.getId(), null);
        return response;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        Instant now = Instant.now();
        String hash = hash(request.refreshToken());
        RefreshToken current = refreshTokens.findByTokenHash(hash).orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (current.getRevokedAt() != null) {
            revokeFamily(current.getFamilyId(), now);
            audit.record(AuditEventType.TOKEN_REUSE_DETECTED, current.getUser().getId(), null);
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (!current.getExpiresAt().isAfter(now)) throw new UnauthorizedException("Invalid refresh token");
        AppUser user = users.findById(current.getUser().getId()).orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (user.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Invalid refresh token");
        current.setRevokedAt(now);
        current.prepareForPersist();
        AuthDtos.TokenResponse response = issueTokens(user, now, current.getFamilyId());
        RefreshToken replacement = refreshTokens.findByTokenHash(hash(response.refreshToken())).orElseThrow();
        current.setReplacedByTokenId(replacement.getId());
        refreshTokens.save(current);
        audit.record(AuditEventType.TOKEN_REFRESH, user.getId(), null);
        return response;
    }

    @Transactional
    public void logout(Authentication authentication, String rawRefreshToken) {
        AppUser user = authenticatedUser(authentication);
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            if (token.getUser().getId().equals(user.getId()) && token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                token.prepareForPersist();
                refreshTokens.save(token);
            }
        });
        audit.record(AuditEventType.LOGOUT, user.getId(), null);
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(Authentication authentication) { return me(authenticatedUser(authentication)); }

    @Transactional
    public void changePassword(Authentication authentication, AuthDtos.ChangePasswordRequest request) {
        AppUser user = authenticatedUser(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.prepareForPersist();
        users.save(user);
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
        audit.record(AuditEventType.PASSWORD_CHANGED, user.getId(), null);
    }

    public AppUser authenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new UnauthorizedException("Authentication is required");
        try {
            AppUser user = users.findById(UUID.fromString(authentication.getName()))
                    .orElseThrow(() -> new UnauthorizedException("Authentication is required"));
            if (user.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Authentication is required");
            return user;
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Authentication is required");
        }
    }

    private AuthDtos.TokenResponse issueTokens(AppUser user, Instant now, UUID familyId) {
        String rawRefresh = randomToken();
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setFamilyId(familyId);
        refresh.setTokenHash(hash(rawRefresh));
        refresh.setIssuedAt(now);
        refresh.setExpiresAt(now.plus(properties.getRefreshTokenTtl()));
        refresh.prepareForPersist();
        refreshTokens.saveAndFlush(refresh);
        return new AuthDtos.TokenResponse(jwtTokens.accessToken(user, now), rawRefresh, "Bearer", jwtTokens.expiresInSeconds());
    }

    private boolean isTemporarilyLocked(AppUser user, Instant now) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) return true;
        if (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.prepareForPersist();
        }
        return false;
    }

    private void recordFailedLogin(AppUser user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= properties.getLogin().getMaxFailedAttempts()) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(now.plus(properties.getLogin().getLockDuration()));
            audit.record(AuditEventType.ACCOUNT_LOCKED, user.getId(), null);
        }
        user.prepareForPersist();
        users.save(user);
    }

    private void revokeFamily(UUID familyId, Instant now) {
        refreshTokens.revokeFamily(familyId, now);
    }

    private AuthDtos.MeResponse me(AppUser user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).sorted().toList();
        List<String> permissionCodes = user.getRoles().stream().flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode()).distinct().sorted().toList();
        return new AuthDtos.MeResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), roleNames, permissionCodes);
    }

    public String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String shortHash(String raw) { return hash(raw).substring(0, 12); }
}
