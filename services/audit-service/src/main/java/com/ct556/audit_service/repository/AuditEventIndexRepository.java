package com.ct556.audit_service.repository;

import com.ct556.audit_service.entity.AuditEventIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditEventIndexRepository extends JpaRepository<AuditEventIndex, UUID>, JpaSpecificationExecutor<AuditEventIndex> {
}
