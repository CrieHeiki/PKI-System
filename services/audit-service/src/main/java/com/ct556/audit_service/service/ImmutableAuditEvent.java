package com.ct556.audit_service.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImmutableAuditEvent(
        UUID id,
        String eventType,
        UUID actorId,
        String actorRole,
        UUID targetId,
        String resourceType,
        UUID resourceId,
        String correlationId,
        String traceId,
        LocalDateTime eventTime,
        String summary,
        JsonNode payload,
        LocalDateTime createdAt
) {
}
