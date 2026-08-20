package com.ct556.audit_service.service;

import com.ct556.audit_service.dto.request.CreateAuditEventRequest;
import com.ct556.audit_service.dto.response.AuditEventResponse;
import com.ct556.audit_service.entity.AuditEventIndex;
import com.ct556.audit_service.entity.AuditEventPayload;
import com.ct556.audit_service.exception.ResourceNotFoundException;
import com.ct556.audit_service.repository.AuditEventIndexRepository;
import com.ct556.audit_service.repository.AuditEventPayloadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventIndexRepository auditEventIndexRepository;
    private final AuditEventPayloadRepository auditEventPayloadRepository;
    private final ImmudbAuditStore immudbAuditStore;

    public AuditService(AuditEventIndexRepository auditEventIndexRepository,
                        AuditEventPayloadRepository auditEventPayloadRepository,
                        ImmudbAuditStore immudbAuditStore) {
        this.auditEventIndexRepository = auditEventIndexRepository;
        this.auditEventPayloadRepository = auditEventPayloadRepository;
        this.immudbAuditStore = immudbAuditStore;
    }

    @Transactional
    public AuditEventResponse createEvent(CreateAuditEventRequest request) {
        UUID eventId = UUID.randomUUID();
        LocalDateTime eventTime = request.getEventTime() == null ? LocalDateTime.now() : request.getEventTime();
        LocalDateTime createdAt = LocalDateTime.now();
        ImmutableAuditEvent immutableEvent = new ImmutableAuditEvent(
                eventId,
                request.getEventType().trim(),
                request.getActorId(),
                trimToNull(request.getActorRole()),
                request.getTargetId(),
                trimToNull(request.getResourceType()),
                request.getResourceId(),
                trimToNull(request.getCorrelationId()),
                trimToNull(request.getTraceId()),
                eventTime,
                trimToNull(request.getSummary()),
                request.getPayload(),
                createdAt
        );

        Long immudbTxId = immudbAuditStore.appendEvent(immutableEvent);

        AuditEventIndex event = new AuditEventIndex();
        event.setId(eventId);
        event.setImmudbTxId(immudbTxId);
        event.setEventType(immutableEvent.eventType());
        event.setActorId(immutableEvent.actorId());
        event.setActorRole(immutableEvent.actorRole());
        event.setTargetId(immutableEvent.targetId());
        event.setResourceType(immutableEvent.resourceType());
        event.setResourceId(immutableEvent.resourceId());
        event.setCorrelationId(immutableEvent.correlationId());
        event.setTraceId(immutableEvent.traceId());
        event.setEventTime(immutableEvent.eventTime());
        event.setSummary(immutableEvent.summary());
        event.setCreatedAt(createdAt);

        auditEventIndexRepository.save(event);

        if (immutableEvent.payload() != null) {
            AuditEventPayload payload = new AuditEventPayload();
            payload.setId(UUID.randomUUID());
            payload.setAuditEventId(event.getId());
            payload.setPayload(immutableEvent.payload());
            payload.setCreatedAt(LocalDateTime.now());
            auditEventPayloadRepository.save(payload);
        }

        return loadEventResponse(event);
    }

    @Transactional
    public AuditEventResponse getEvent(UUID eventId) {
        AuditEventIndex event = auditEventIndexRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit event not found: " + eventId));

        return loadEventResponse(event);
    }

    @Transactional
    public List<AuditEventResponse> searchEvents(String eventType,
                                                 UUID actorId,
                                                 String resourceType,
                                                 UUID resourceId,
                                                 String correlationId,
                                                 LocalDateTime from,
                                                 LocalDateTime to,
                                                 int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        Specification<AuditEventIndex> specification = Specification.unrestricted();
        specification = andIfPresent(specification, equalsIfPresent("eventType", trimToNull(eventType)));
        specification = andIfPresent(specification, equalsIfPresent("actorId", actorId));
        specification = andIfPresent(specification, equalsIfPresent("resourceType", trimToNull(resourceType)));
        specification = andIfPresent(specification, equalsIfPresent("resourceId", resourceId));
        specification = andIfPresent(specification, equalsIfPresent("correlationId", trimToNull(correlationId)));
        specification = andIfPresent(specification, fromIfPresent(from));
        specification = andIfPresent(specification, toIfPresent(to));

        return auditEventIndexRepository.findAll(
                        specification,
                        PageRequest.of(0, normalizedLimit, Sort.by(Sort.Direction.DESC, "eventTime"))
                )
                .stream()
                .map(this::loadEventResponse)
                .toList();
    }

    private Specification<AuditEventIndex> andIfPresent(Specification<AuditEventIndex> base,
                                                        Specification<AuditEventIndex> candidate) {
        return candidate == null ? base : base.and(candidate);
    }

    private Specification<AuditEventIndex> equalsIfPresent(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(fieldName), value);
    }

    private Specification<AuditEventIndex> fromIfPresent(LocalDateTime from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventTime"), from);
    }

    private Specification<AuditEventIndex> toIfPresent(LocalDateTime to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventTime"), to);
    }

    private AuditEventResponse toResponse(AuditEventIndex event, JsonNode payload) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .immudbTxId(event.getImmudbTxId())
                .eventType(event.getEventType())
                .actorId(event.getActorId())
                .actorRole(event.getActorRole())
                .targetId(event.getTargetId())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .correlationId(event.getCorrelationId())
                .traceId(event.getTraceId())
                .eventTime(event.getEventTime())
                .summary(event.getSummary())
                .payload(payload)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private AuditEventResponse loadEventResponse(AuditEventIndex event) {
        if (event.getImmudbTxId() != null && immudbAuditStore.isEnabled()) {
            ImmutableAuditEvent immutableEvent = immudbAuditStore.getEvent(event.getId(), event.getImmudbTxId())
                    .orElseThrow(() -> new IllegalStateException("Audit event missing in ImmuDB: " + event.getId()));
            return toResponse(event, immutableEvent.payload());
        }

        JsonNode payload = auditEventPayloadRepository.findFirstByAuditEventId(event.getId())
                .map(AuditEventPayload::getPayload)
                .orElse(null);

        return toResponse(event, payload);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
