package com.shop.auth.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findById(UUID id);

    boolean existsByEmailIgnoreCase(String email);
}
