package com.shop.auth.recovery;

import com.shop.auth.audit.AuditEventType;
import com.shop.auth.audit.AuditService;
import com.shop.auth.authentication.AuthDtos;
import com.shop.auth.authentication.AuthService;
import com.shop.auth.config.AuthProperties;
import com.shop.auth.notification.NotificationSender;
import com.shop.auth.user.AppUser;
import com.shop.auth.user.UserRepository;
import com.shop.auth.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordRecoveryService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final AuthService auth;
    private final AuthProperties properties;
    private final NotificationSender notifications;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public PasswordRecoveryService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder passwords,
                                  AuthService auth, AuthProperties properties, NotificationSender notifications, AuditService audit) {
        this.users = users; this.tokens = tokens; this.passwords = passwords; this.auth = auth; this.properties = properties; this.notifications = notifications; this.audit = audit;
    }

    @Transactional
    public void request(AuthDtos.ForgotPasswordRequest request) {
        String email = auth.normalizeEmail(request.email());
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) return;
        String raw = randomToken();
        PasswordResetToken token = new PasswordResetToken(); token.setUser(user); token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(properties.getPasswordReset().getTokenTtl())); tokens.save(token);
        notifications.sendPasswordReset(email, properties.getPasswordReset().getBaseUrl() + "?token=" + raw);
    }

    @Transactional
    public void reset(AuthDtos.ResetPasswordRequest request) {
        PasswordResetToken token = tokens.findByTokenHash(hash(request.token())).orElseThrow(() -> new com.shop.auth.common.UnauthorizedException("Reset token is invalid or expired"));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) throw new com.shop.auth.common.UnauthorizedException("Reset token is invalid or expired");
        AppUser user = token.getUser();
        user.setPasswordHash(passwords.encode(request.newPassword())); user.prepareForPersist(); users.save(user);
        token.setUsedAt(now); tokens.save(token);
        auth.revokeAllSessions(user.getId());
        audit.record(AuditEventType.PASSWORD_CHANGED, user.getId(), "password_reset=true");
    }

    private String randomToken() { byte[] bytes = new byte[48]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
}
