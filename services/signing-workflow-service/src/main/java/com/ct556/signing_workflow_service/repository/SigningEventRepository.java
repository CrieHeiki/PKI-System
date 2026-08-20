package com.ct556.signing_workflow_service.repository;

import com.ct556.signing_workflow_service.entity.SigningEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SigningEventRepository extends JpaRepository<SigningEvent, UUID> {

    List<SigningEvent> findBySigningRequestIdOrderByEventTimeAsc(UUID signingRequestId);
}