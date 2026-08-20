package com.ct556.audit_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_event_index")
public class AuditEventIndex {

    @Id
    private UUID id;

    @Column(name = "immudb_tx_id")
    private Long immudbTxId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "trace_id", length = 255)
    private String traceId;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
