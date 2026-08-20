package com.ct556.signing_workflow_service.client;

import java.util.Map;
import java.util.UUID;

public interface AuditGateway {
    void publishEvent(String eventType,
                      UUID actorId,
                      String actorRole,
                      UUID targetId,
                      String resourceType,
                      UUID resourceId,
                      String correlationId,
                      String summary,
                      Map<String, Object> payload);
}
