package com.shop.auth.config;

import com.shop.auth.role.Role;
import com.shop.auth.role.RoleRepository;
import com.shop.auth.user.AppUser;
import com.shop.auth.user.UserRepository;
import com.shop.auth.user.UserStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {
    private final AuthProperties properties;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public InitialAdminBootstrap(AuthProperties properties, UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.properties = properties;
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.getBootstrap().getAdminEmail();
        String password = properties.getBootstrap().getAdminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalized)) return;
        Role superAdmin = roles.findByName("SUPER_ADMIN").orElseThrow();
        AppUser admin = new AppUser();
        admin.setEmail(normalized);
        admin.setPasswordHash(encoder.encode(password));
        admin.setFirstName("Initial");
        admin.setLastName("Administrator");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin.setRoles(new HashSet<>());
        admin.getRoles().add(superAdmin);
        admin.prepareForPersist();
        users.save(admin);
    }
}
