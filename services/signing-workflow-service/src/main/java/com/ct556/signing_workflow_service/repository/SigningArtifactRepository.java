package com.ct556.signing_workflow_service.repository;

import com.ct556.signing_workflow_service.entity.SigningArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SigningArtifactRepository extends JpaRepository<SigningArtifact, UUID> {
    Optional<SigningArtifact> findBySigningRequestId(UUID signingRequestId);
    boolean existsBySigningRequestId(UUID signingRequestId);
}