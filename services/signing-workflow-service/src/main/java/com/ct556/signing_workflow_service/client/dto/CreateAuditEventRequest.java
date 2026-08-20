package com.ct556.signing_workflow_service.client.dto;

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
