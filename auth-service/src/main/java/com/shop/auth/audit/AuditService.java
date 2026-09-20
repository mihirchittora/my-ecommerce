package com.shop.auth.audit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {
    private final AuthAuditEventRepository repository;

    public AuditService(AuthAuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(AuditEventType type, UUID userId, String metadata) {
        AuthAuditEvent event = new AuthAuditEvent();
        event.setEventType(type);
        event.setUserId(userId);
        event.setOccurredAt(Instant.now());
        event.setMetadata(toJsonMetadata(metadata));
        repository.save(event);
    }

    private String toJsonMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        if (metadata.trim().startsWith("{")) return metadata;
        int separator = metadata.indexOf('=');
        String key = separator > 0 ? metadata.substring(0, separator) : "value";
        String value = separator > 0 ? metadata.substring(separator + 1) : metadata;
        return "{\"" + escape(key) + "\":\"" + escape(value) + "\"}";
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
