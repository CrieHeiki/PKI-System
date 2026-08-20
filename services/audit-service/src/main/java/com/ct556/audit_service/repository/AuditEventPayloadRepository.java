package com.ct556.audit_service.repository;

import com.ct556.audit_service.entity.AuditEventPayload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditEventPayloadRepository extends JpaRepository<AuditEventPayload, UUID> {
    Optional<AuditEventPayload> findFirstByAuditEventId(UUID auditEventId);
}
