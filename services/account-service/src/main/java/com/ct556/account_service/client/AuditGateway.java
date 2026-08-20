package com.ct556.account_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
public class AuditGateway {

    private static final Logger log = LoggerFactory.getLogger(AuditGateway.class);

    private final RestClient restClient;

    public AuditGateway(@Value("${services.audit-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void publishEvent(String eventType,
                             UUID actorId,
                             String actorRole,
                             UUID targetId,
                             String resourceType,
                             UUID resourceId,
                             String correlationId,
                             String summary,
                             Map<String, Object> payload) {
        try {
            restClient.post()
                    .uri("/internal/v1/audit/events")
                    .body(new CreateAuditEventRequest(
                            eventType,
                            actorId,
                            actorRole,
                            targetId,
                            resourceType,
                            resourceId,
                            correlationId,
                            null,
                            summary,
                            payload
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to publish audit event {} for resource {}", eventType, resourceId, ex);
        }
    }
}
