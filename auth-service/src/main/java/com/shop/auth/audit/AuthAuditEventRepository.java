package com.shop.auth.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, UUID> {
}
