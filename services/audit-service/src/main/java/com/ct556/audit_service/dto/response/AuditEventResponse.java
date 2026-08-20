package com.ct556.audit_service.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditEventResponse {
    private UUID id;
    private Long immudbTxId;
    private String eventType;
    private UUID actorId;
    private String actorRole;
    private UUID targetId;
    private String resourceType;
    private UUID resourceId;
    private String correlationId;
    private String traceId;
    private LocalDateTime eventTime;
    private String summary;
    private JsonNode payload;
    private LocalDateTime createdAt;
}
