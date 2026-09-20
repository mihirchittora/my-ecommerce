package com.shop.auth.user;

import com.shop.auth.audit.AuditEventType;
import com.shop.auth.audit.AuditService;
import com.shop.auth.common.ConflictException;
import com.shop.auth.common.NotFoundException;
import com.shop.auth.role.Role;
import com.shop.auth.role.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class UserAdminService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAdminService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional
    public UserAdminDtos.Response create(UserAdminDtos.CreateRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("An account already exists for this email");
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>());
        if (request.roleIds() != null) request.roleIds().forEach(id -> user.getRoles().add(role(id)));
        if (user.getRoles().isEmpty()) user.getRoles().add(roleByName("CUSTOMER"));
        user.prepareForPersist();
        return toResponse(users.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserAdminDtos.Response> list(Pageable pageable) { return users.findAll(pageable).map(this::toResponse); }

    @Transactional(readOnly = true)
    public UserAdminDtos.Response get(UUID id) { return toResponse(users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id))); }

    @Transactional
    public UserAdminDtos.Response update(UUID id, UserAdminDtos.UpdateRequest request) {
        AppUser user = user(id);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.prepareForPersist();
        return toResponse(user);
    }

    @Transactional
    public UserAdminDtos.Response status(UUID id, UserAdminDtos.StatusRequest request) {
        AppUser user = user(id);
        try {
            user.setStatus(UserStatus.valueOf(request.status().trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported user status");
        }
        if (user.getStatus() != UserStatus.LOCKED) user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.INACTIVE) audit.record(AuditEventType.ACCOUNT_DISABLED, id, null);
        user.prepareForPersist();
        return toResponse(user);
    }

    @Transactional
    public UserAdminDtos.Response addRole(UUID userId, UUID roleId) {
        AppUser user = user(userId);
        Role role = role(roleId);
        user.getRoles().add(role);
        audit.record(AuditEventType.ROLE_ASSIGNED, userId, "role=" + role.getName());
        user.prepareForPersist();
        return toResponse(user);
    }

    @Transactional
    public UserAdminDtos.Response removeRole(UUID userId, UUID roleId) {
        AppUser user = user(userId);
        Role role = role(roleId);
        user.getRoles().removeIf(item -> item.getId().equals(role.getId()));
        audit.record(AuditEventType.ROLE_REMOVED, userId, "role=" + role.getName());
        user.prepareForPersist();
        return toResponse(user);
    }

    private AppUser user(UUID id) { return users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id)); }
    private Role role(UUID id) { return roles.findById(id).orElseThrow(() -> new NotFoundException("Role not found: " + id)); }
    private Role roleByName(String name) { return roles.findByName(name).orElseThrow(() -> new IllegalStateException(name + " role is not seeded")); }
    private UserAdminDtos.Response toResponse(AppUser user) {
        return new UserAdminDtos.Response(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getStatus(), user.isEmailVerified(),
                user.getRoles().stream().map(Role::getName).sorted().toList());
    }
}
