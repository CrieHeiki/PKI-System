package com.ct556.document_service.client;

import java.util.Map;
import java.util.UUID;

public record CreateAuditEventRequest(
        String eventType,
        UUID actorId,
        String actorRole,
        UUID targetId,
        String resourceType,
        UUID resourceId,
        String correlationId,
        String traceId,
        String summary,
        Map<String, Object> payload
) {
}
