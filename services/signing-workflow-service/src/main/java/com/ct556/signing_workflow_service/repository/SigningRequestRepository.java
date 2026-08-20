package com.ct556.signing_workflow_service.repository;

import com.ct556.signing_workflow_service.domain.SigningRequestStatus;
import com.ct556.signing_workflow_service.entity.SigningRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

public interface SigningRequestRepository extends JpaRepository<SigningRequest, UUID> {

    List<SigningRequest> findByRequesterAccountIdOrderByCreatedAtDesc(UUID requesterAccountId);

    List<SigningRequest> findBySignerAccountIdAndStatusOrderByCreatedAtDesc(UUID signerAccountId, SigningRequestStatus status);

    List<SigningRequest> findBySignerAccountIdAndStatusInOrderByCreatedAtDesc(UUID signerAccountId, Collection<SigningRequestStatus> statuses);

    List<SigningRequest> findAllByOrderByCreatedAtDesc();
}
