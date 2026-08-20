package com.ct556.audit_service.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CreateAuditEventRequest {

    @NotBlank(message = "eventType is required")
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
}
